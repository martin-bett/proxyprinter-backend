package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Liest Kartenbilder robust ein. Einzelne Decoder (JDK, TwelveMonkeys) scheitern an manchen Dateien mit
 * Laufzeitfehlern wie "Coordinate out of bounds!", obwohl ein anderer Decoder oder das reine Raster ohne
 * Farbprofil das Bild lesen kann. Deshalb werden nacheinander alle passenden Reader probiert.
 */
@Slf4j
public final class ImageDecoder {

    private ImageDecoder() {
    }

    public static BufferedImage read(File file) throws IOException {
        Exception first;
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                return image;
            }
            first = new IOException("Bildformat wird nicht unterstützt");
        } catch (Exception e) {
            first = e;
        }
        log.warn("ImageIO konnte {} nicht lesen, probiere alle Reader einzeln", file, first);

        IOException failure = new IOException(describe(first), first);
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                throw failure;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                String name = reader.getClass().getName();
                try {
                    in.seek(0);
                    reader.setInput(in, true, true);
                    try {
                        return reader.read(0);
                    } catch (Exception e) {
                        failure.addSuppressed(e);
                        log.debug("{} kann {} nicht lesen: {}", name, file, e.toString());
                    }
                    // Ohne Farbkonvertierung: das reine Raster, z.B. bei kaputtem oder unpassendem ICC-Profil
                    if (reader.canReadRaster()) {
                        in.seek(0);
                        reader.setInput(in, true, true);
                        try {
                            BufferedImage image = fromRaster(reader.readRaster(0, null));
                            if (image != null) {
                                log.info("{} über das Raster von {} gelesen", file, name);
                                return image;
                            }
                        } catch (Exception e) {
                            failure.addSuppressed(e);
                            log.debug("{} kann das Raster von {} nicht lesen: {}", name, file, e.toString());
                        }
                    }
                } finally {
                    reader.dispose();
                }
            }
        }
        throw failure;
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getClass().getSimpleName() + ": " + e.getMessage() : e.toString();
    }

    /** Baut aus einem 8-Bit-Raster mit 1, 3 oder 4 Bändern (Grau, RGB, RGBA) ein sRGB-Bild. */
    static BufferedImage fromRaster(Raster raster) {
        int bands = raster.getNumBands();
        if (bands != 1 && bands != 3 && bands != 4) {
            return null;
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        BufferedImage image = new BufferedImage(w, h, bands == 4 ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int[] px = new int[bands];
        int shift = Math.max(0, raster.getSampleModel().getSampleSize(0) - 8);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.getPixel(raster.getMinX() + x, raster.getMinY() + y, px);
                int r = clamp(px[0] >> shift);
                int g = bands == 1 ? r : clamp(px[1] >> shift);
                int b = bands == 1 ? r : clamp(px[2] >> shift);
                int a = bands == 4 ? clamp(px[3] >> shift) : 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    private static int clamp(int v) {
        return Math.min(255, Math.max(0, v));
    }
}
