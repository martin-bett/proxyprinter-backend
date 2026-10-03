package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

@Service
public class BufferedImageCacheService {
    
    // Die Map speichert die URL als Schlüssel und das BufferedImage mit einem Zeitstempel als Wert
    private final Map<String, CachedImage> cache = new ConcurrentHashMap<>();
    
    // Zeit in Millisekunden, nach der nicht verwendete Bilder entfernt werden (z.B. 5 Minuten)
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(30);
    
    // Executor für das regelmäßige Bereinigen des Caches
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public BufferedImageCacheService() {
        // Starte den Hintergrund-Task zum Bereinigen des Caches
        scheduler.scheduleAtFixedRate(this::removeExpiredEntries, 0, 1, TimeUnit.MINUTES);
    }

    // Methode zum Hinzufügen eines Bildes zum Cache
    public void addImage(String imageUrl, BufferedImage image) {
        cache.put(imageUrl, new CachedImage(image, System.currentTimeMillis()));
    }

    // Methode zum Abrufen eines Bildes aus dem Cache
    public BufferedImage getImage(String imageUrl) {
        CachedImage cachedImage = cache.get(imageUrl);
        if (cachedImage != null) {
            // Aktualisiere den Zugriffstimestamp
            cachedImage.setLastAccessed(System.currentTimeMillis());
            return cachedImage.getImage();
        }
        return null;
    }

    // Methode zum Entfernen von abgelaufenen Bildern
    private void removeExpiredEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CachedImage>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedImage> entry = iterator.next();
            if (now - entry.getValue().getLastAccessed() > CACHE_EXPIRATION_TIME) {
                iterator.remove();
            }
        }
    }

    // Hilfsklasse, um das Bild und den Zeitstempel zu speichern
    private static class CachedImage {
        private final BufferedImage image;
        private long lastAccessed;

        public CachedImage(BufferedImage image, long lastAccessed) {
            this.image = image;
            this.lastAccessed = lastAccessed;
        }

        public BufferedImage getImage() {
            return image;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public void setLastAccessed(long lastAccessed) {
            this.lastAccessed = lastAccessed;
        }
    }
}
