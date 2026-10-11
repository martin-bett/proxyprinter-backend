package my.poo.mtgcrawl.mtgprint.service.rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.FileUploadResult;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.exc.FileWriteException;
import my.poo.mtgcrawl.mtgprint.service.rest.exc.WrongFormatException;

@Service
@Slf4j
public class FileUploadService {
    
    public static final String IMG_TEMP_FOLDER = "img/temp/";
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;
    private final HashService hashService;
    private volatile long maxAgeMs;

    public FileUploadService(HashService hashService, @Value("${upload.retention-days:30}") int retentionDays) {
        this.hashService = hashService;
        this.maxAgeMs = Math.max(1, retentionDays) * DAY_MS;
        try {
            Files.createDirectories(Paths.get(IMG_TEMP_FOLDER));
        } catch (IOException e) {
            log.error("Konnte Upload-Ordner {} nicht anlegen", IMG_TEMP_FOLDER, e);
        }
    }

    public int getRetentionDays() {
        return (int) (maxAgeMs / DAY_MS);
    }

    /** Neue Aufbewahrungszeit (Admin-Einstellung), gilt ab dem nächsten stündlichen Aufräumen. */
    public void setRetentionDays(int retentionDays) {
        this.maxAgeMs = Math.max(1, retentionDays) * DAY_MS;
    }

    /** Löscht alle hochgeladenen Bilder sofort (Admin: "Jetzt leeren"). Snapshots, die sie nutzen, verlieren sie. */
    public int clear() {
        File[] files = new File(IMG_TEMP_FOLDER).listFiles(File::isFile);
        int deleted = 0;
        if (files != null) {
            for (File f : files) {
                if (f.delete()) {
                    deleted++;
                }
            }
        }
        log.info("Cleared {} uploaded images", deleted);
        return deleted;
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void clearTempFiles(){
        File[] files = new File(IMG_TEMP_FOLDER).listFiles();
        if (files == null) {
            return;
        }
        for(File f : files){
            if(new Date().getTime() - f.lastModified() >= maxAgeMs){
                f.delete();
            }                
        }
    }

    /** Anzahl der hochgeladenen Bilder, die gerade aufbewahrt werden, für /api/stats. */
    public long countTempFiles() {
        File[] files = new File(IMG_TEMP_FOLDER).listFiles(File::isFile);
        return files == null ? 0 : files.length;
    }

    /**
     * Setzt die Aufbewahrungszeit eines hochgeladenen Bildes zurück. Das Frontend ruft das
     * auf, wenn ein Snapshot mit diesem Bild gespeichert oder geladen wird, damit das Bild
     * erst upload.retention-days nach der letzten Nutzung gelöscht wird.
     *
     * @return false, wenn es die Datei nicht (mehr) gibt
     */
    public boolean touch(String generatedId) {
        Path folder = Paths.get(IMG_TEMP_FOLDER).toAbsolutePath().normalize();
        Path file = folder.resolve(generatedId).normalize();
        if (!file.getParent().equals(folder) || !Files.isRegularFile(file)) {
            return false;
        }
        return file.toFile().setLastModified(System.currentTimeMillis());
    }

    public FileUploadResult uploadFile(MultipartFile file){
        FileUploadResult fur = new FileUploadResult();
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = generateUniqueFileName(originalFilename) + fileExtension;
        fur.setGeneratedId(uniqueFileName);
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        }catch(Exception e){
            fur.setSuccess(false);
            fur.setException(new WrongFormatException(e));
        }
        try {
             Files.createDirectories(Paths.get(IMG_TEMP_FOLDER));
            File outputFile = new File(IMG_TEMP_FOLDER + uniqueFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 InputStream inputStream = new ByteArrayInputStream(file.getBytes())) {
                IOUtils.copy(inputStream, fos);
                fur.setSuccess(true);
            }
        } catch (Exception e) {
            fur.setSuccess(false);
            fur.setException(new FileWriteException(e));
        }
        return fur;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension found
        }
        return filename.substring(lastIndexOf);
    }

        private String generateUniqueFileName(String originalFileName) {
        String baseFileName = hashService.curFileName(originalFileName);
        String uniqueID = UUID.randomUUID().toString();
        return baseFileName + "-" + uniqueID;
    }
}
