/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PdfCacheService {
    private final String pdfCachePath = "pdf/cache/";
    private Map<String, Date> timeStampMap = new ConcurrentHashMap<String, Date>();

    public PdfCacheService() {
        File cacheDir = new File("pdf/cache/");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public void addToCache(String progressId, byte[] pdfContent) {
        String filePath = "pdf/cache/" + progressId + ".pdf";
        try {
            Files.write(Paths.get(filePath, new String[0]), pdfContent, new OpenOption[0]);
            this.timeStampMap.put(progressId, new Date());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getFromCache(String progressId) {
        String filePath = "pdf/cache/" + progressId + ".pdf";
        try {
            if (this.timeStampMap.containsKey(progressId)) {
                return Files.readAllBytes(Paths.get(filePath, new String[0]));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Scheduled(fixedRate=3600000L)
    public void cleanUpCache() {
        Date now = new Date();
        this.timeStampMap.entrySet().removeIf(entry -> {
            String progressId = (String)entry.getKey();
            Date timestamp = (Date)entry.getValue();
            long diffInMillis = now.getTime() - timestamp.getTime();
            if (diffInMillis > 86400000L) {
                String filePath = "pdf/cache/" + progressId + ".pdf";
                try {
                    Files.deleteIfExists(Paths.get(filePath, new String[0]));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        });
    }

    public Integer getCachedPdfCount() {
        return this.timeStampMap.values().stream().filter(ts -> new Date().getTime() - ts.getTime() < 86400000L).toList().size();
    }
}

