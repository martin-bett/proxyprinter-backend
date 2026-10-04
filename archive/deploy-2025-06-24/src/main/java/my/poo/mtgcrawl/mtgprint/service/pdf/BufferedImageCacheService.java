/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class BufferedImageCacheService {
    private final Map<String, CachedImage> cache = new ConcurrentHashMap<String, CachedImage>();
    private static final long CACHE_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(30L);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public BufferedImageCacheService() {
        this.scheduler.scheduleAtFixedRate(this::removeExpiredEntries, 0L, 15L, TimeUnit.MINUTES);
    }

    public void addImage(String imageUrl, BufferedImage image) {
        this.cache.put(imageUrl, new CachedImage(image, System.currentTimeMillis()));
    }

    public BufferedImage getImage(String imageUrl) {
        CachedImage cachedImage = this.cache.get(imageUrl);
        if (cachedImage != null) {
            cachedImage.setLastAccessed(System.currentTimeMillis());
            return cachedImage.getImage();
        }
        return null;
    }

    private void removeExpiredEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CachedImage>> iterator = this.cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedImage> entry = iterator.next();
            if (now - entry.getValue().getLastAccessed() <= CACHE_EXPIRATION_TIME) continue;
            iterator.remove();
        }
    }

    public Integer getMemoryImageCount() {
        return this.cache.size();
    }

    public Long getMemoryImageSize() {
        return this.cache.values().stream().mapToLong(cachedImage -> {
            BufferedImage img = cachedImage.getImage();
            return (long)img.getWidth() * (long)img.getHeight() * (long)(switch (img.getType()) {
                case 5 -> 3;
                case 6, 7 -> 4;
                case 10 -> 1;
                case 11 -> 2;
                case 8, 9 -> 2;
                case 1, 2, 3, 4 -> 4;
                default -> 4;
            });
        }).sum();
    }

    private static class CachedImage {
        private final BufferedImage image;
        private long lastAccessed;

        public CachedImage(BufferedImage image, long lastAccessed) {
            this.image = image;
            this.lastAccessed = lastAccessed;
        }

        public BufferedImage getImage() {
            return this.image;
        }

        public long getLastAccessed() {
            return this.lastAccessed;
        }

        public void setLastAccessed(long lastAccessed) {
            this.lastAccessed = lastAccessed;
        }
    }
}

