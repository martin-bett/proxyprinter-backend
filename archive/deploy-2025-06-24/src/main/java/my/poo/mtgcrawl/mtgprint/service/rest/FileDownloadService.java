/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FileDownloadService {
    private static final Logger log = LoggerFactory.getLogger(FileDownloadService.class);
    private final RestTemplate restTemplate;
    public static final String IMG_CACHE_FOLDER = "img/cache/";
    private final Tika tika = new Tika();
    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(4);
    private final PriorityBlockingQueue<DownloadTask> downloadQueue = new PriorityBlockingQueue();
    private final Set<String> downloading = ConcurrentHashMap.newKeySet();

    public FileDownloadService(RestTemplate r) {
        new File(IMG_CACHE_FOLDER).mkdirs();
        this.restTemplate = r;
        this.startDownloadWorker();
    }

    private void startDownloadWorker() {
        Runnable worker = () -> {
            while (true) {
                try {
                    while (true) {
                        DownloadTask task = this.downloadQueue.take();
                        if (this.downloading.add(task.url)) {
                            try {
                                task.callback.accept(this.downloadFileInternal(task.url, task.fileName));
                            }
                            finally {
                                this.downloading.remove(task.url);
                            }
                            continue;
                        }
                        task.callback.accept(this.checkFileExists(task.fileName));
                    }
                }
                catch (Exception e) {
                    log.error("Download worker error", e);
                    continue;
                }
                break;
            }
        };
        for (int i = 0; i < 4; ++i) {
            this.downloadExecutor.submit(worker);
        }
    }

    public void queueDownload(String url, String fileName, DownloadPriority priority, Consumer<String> callback) {
        String resolved = this.resolveLocalOrCached(url);
        if (resolved != null) {
            callback.accept(resolved);
            return;
        }
        this.downloadQueue.offer(new DownloadTask(url, fileName, priority, callback));
    }

    public String resolveLocalOrCached(String url) {
        String filePath;
        File f;
        if (url == null) {
            return null;
        }
        if (url.startsWith("/api/files/download/") && (f = new File(filePath = "img/temp/" + url.replace("/api/files/download/", ""))).exists()) {
            return filePath;
        }
        String fileName = this.hashFileName(url);
        String existing = this.checkFileExists(fileName);
        if (existing != null) {
            return existing;
        }
        return null;
    }

    public String downloadFile(String url, String fileName) {
        String resolved = this.resolveLocalOrCached(url);
        if (resolved != null) {
            return resolved;
        }
        return this.downloadFileInternal(url, fileName);
    }

    private String downloadFileInternal(String url, String fileName) {
        String finalUrl = this.followRedirects(url);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<byte[]> response = this.restTemplate.exchange(finalUrl, HttpMethod.GET, entity, byte[].class, new Object[0]);
        if (response.getStatusCode() == HttpStatus.OK) {
            byte[] fileContent = (byte[])response.getBody();
            return this.saveByteArray(fileContent, fileName);
        }
        log.error("Failed to download file. Status code: " + String.valueOf(response.getStatusCode()));
        return null;
    }

    public String checkFileExists(String fileName) {
        Path baseFilePath = Paths.get(IMG_CACHE_FOLDER + fileName, new String[0]);
        try {
            Optional<Path> existingFile = Files.list(baseFilePath.getParent()).filter(path -> path.getFileName().toString().startsWith(fileName + ".")).findFirst();
            if (existingFile.isPresent()) {
                log.debug("File already exists: " + String.valueOf(existingFile.get()));
                return existingFile.get().toString();
            }
        }
        catch (IOException e) {
            log.error("Error checking for existing file: " + e.getMessage(), e);
        }
        return null;
    }

    public String saveByteArray(byte[] fileContent, String fileName) {
        try {
            String mimeType = this.tika.detect(fileContent);
            String extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
            String fullFileName = IMG_CACHE_FOLDER + fileName + extension;
            Path filePath = Paths.get(fullFileName, new String[0]);
            Files.createDirectories(filePath.getParent(), new FileAttribute[0]);
            Files.write(filePath, fileContent, new OpenOption[0]);
            log.debug("File downloaded successfully: " + fullFileName);
            return fullFileName;
        }
        catch (Exception e) {
            log.error("Error writing file: " + e.getMessage(), e);
            return null;
        }
    }

    private String followRedirects(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.GET, entity, String.class, new Object[0]);
        if (response.getStatusCode().is3xxRedirection()) {
            String newUrl = response.getHeaders().getLocation().toString();
            return this.followRedirects(newUrl);
        }
        return url;
    }

    public Integer getCachedImageCount() {
        try {
            return (int)Files.list(Paths.get(IMG_CACHE_FOLDER, new String[0])).filter(path -> !Files.isDirectory(path, new LinkOption[0])).count();
        }
        catch (IOException e) {
            log.error("Error counting images in cache: " + e.getMessage(), e);
            return 0;
        }
    }

    public void preloadImages(List<String> imageUrls) {
        for (String url : imageUrls) {
            String fileName = this.hashFileName(url);
            this.queueDownload(url, fileName, DownloadPriority.PRELOAD, result -> {});
        }
    }

    public String hashFileName(String url) {
        return Integer.toHexString(url.hashCode());
    }

    private static class DownloadTask
    implements Comparable<DownloadTask> {
        final String url;
        final String fileName;
        final DownloadPriority priority;
        final Consumer<String> callback;

        DownloadTask(String url, String fileName, DownloadPriority priority, Consumer<String> callback) {
            this.url = url;
            this.fileName = fileName;
            this.priority = priority;
            this.callback = callback;
        }

        @Override
        public int compareTo(DownloadTask o) {
            return Integer.compare(this.priority.getPrio(), o.priority.getPrio());
        }
    }

    public static enum DownloadPriority {
        GENERATION(1),
        PROXY(2),
        PRELOAD(3);

        private final int prio;

        private DownloadPriority(int prio) {
            this.prio = prio;
        }

        public int getPrio() {
            return this.prio;
        }
    }
}

