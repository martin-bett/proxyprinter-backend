/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.pdf.DocumentGenerator;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfJob;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PDFGenerationService {
    private final DocumentGenerator documentGenerator;
    private final FileDownloadService fileDownloadService;
    private final PdfCacheService pdfCacheService;
    private final HashService hashService;

    public PDFGenerationService(DocumentGenerator latexDocumentGenerator, FileDownloadService fileDownloadService, HashService hashService, PdfCacheService pdfCacheService) {
        this.documentGenerator = latexDocumentGenerator;
        this.fileDownloadService = fileDownloadService;
        this.pdfCacheService = pdfCacheService;
        this.hashService = hashService;
    }

    public void generateDocument(PdfJob job) throws Exception {
        try {
            ArrayList<ImageDto> processedImages = new ArrayList<ImageDto>();
            int totalFiles = job.request.getImages().size();
            int totalFileSize = 0;
            for (int i = 0; i < totalFiles; ++i) {
                ImageDto image = job.request.getImages().get(i);
                String url = image.getImageUrl();
                CropSettingsDto cropSettings = image.getCropSettings();
                if (url == null) continue;
                String filePath = null;
                int fileSize = 0;
                String[] resultFile = new String[]{null};
                CountDownLatch latch = new CountDownLatch(1);
                this.fileDownloadService.queueDownload(url, this.hashService.curFileName(url), FileDownloadService.DownloadPriority.GENERATION, resolved -> {
                    resultFile[0] = resolved;
                    latch.countDown();
                });
                latch.await(30L, TimeUnit.SECONDS);
                filePath = resultFile[0];
                if (filePath != null) {
                    File f = new File(filePath);
                    if (f.exists()) {
                        fileSize = (int)f.length();
                    }
                    this.sendProgress(job::sendProgress, job.progressId, ProgressDto.Category.download, "Downloaded file " + (i + 1) + " of " + totalFiles, i + 1, totalFiles, null, totalFileSize += fileSize);
                    ImageDto processedImage = new ImageDto();
                    processedImage.setImageUrl(filePath);
                    processedImage.setCropSettings(cropSettings);
                    processedImages.add(processedImage);
                    continue;
                }
                this.sendProgress(job::sendProgress, job.progressId, ProgressDto.Category.download, "Failed to download file " + (i + 1), i + 1, totalFiles, null, totalFileSize);
            }
            this.sendProgress(job::sendProgress, job.progressId, ProgressDto.Category.build, "Starting PDF compilation", 0, 0, null, 0);
            byte[] pdfData = this.documentGenerator.generateDocument(processedImages, job.request.getSettings(), job.progressId, job::sendProgress);
            this.pdfCacheService.addToCache(job.progressId, pdfData);
            this.sendProgress(job::sendProgress, job.progressId, ProgressDto.Category.finishing, "PDF compilation completed", 1, 1, null, pdfData.length);
        }
        catch (Exception e) {
            this.sendProgress(job::sendProgress, job.progressId, ProgressDto.Category.finishing, "Error: " + e.getMessage(), 0, 0, null, 0);
            throw e;
        }
    }

    @Async
    public void preLoadImages(List<String> imageUrls) {
        for (String url : imageUrls) {
            try {
                this.fileDownloadService.downloadFile(url, this.hashService.curFileName(url));
            }
            catch (Exception exception) {}
        }
    }

    private void sendProgress(Consumer<ProgressDto> progressCallback, String progressId, ProgressDto.Category category, String message, int current, int total, Integer pageCount, int fileSize) {
        ProgressDto dto = new ProgressDto();
        dto.setProgressId(progressId);
        dto.setCategory(category);
        dto.setMessage(message);
        dto.setCurrent(current);
        dto.setTotal(total);
        dto.setPageCount(pageCount);
        dto.setFileSize(fileSize);
        try {
            progressCallback.accept(dto);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

