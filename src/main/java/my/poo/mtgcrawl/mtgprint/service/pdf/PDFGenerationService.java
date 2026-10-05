package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto.Category;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;

/**
 * Baut ein PDF in den Phasen download → build → finishing und meldet den
 * Fortschritt an den {@link PdfJob}. Die Warteschlange liegt im
 * {@link PDFGenerationManagerService}.
 */
@Service
public class PDFGenerationService {

    private final DocumentGenerator latexDocumentGenerator;
    private final FileDownloadService fileDownloadService;
    private final HashService hashService;
    private final PdfCacheService pdfCacheService;
    private final PdfSizeLimit pdfSizeLimit;

    public PDFGenerationService(DocumentGenerator latexDocumentGenerator, FileDownloadService fileDownloadService,
            HashService hashService, PdfCacheService pdfCacheService, PdfSizeLimit pdfSizeLimit) {
        this.latexDocumentGenerator = latexDocumentGenerator;
        this.fileDownloadService = fileDownloadService;
        this.hashService = hashService;
        this.pdfCacheService = pdfCacheService;
        this.pdfSizeLimit = pdfSizeLimit;
    }

    public void generateDocument(PdfJob job) throws Exception {
        String progressId = job.progressId;
        CreatePdfDto request = job.request;
        List<ImageDto> processedImages = new ArrayList<>();
        int totalFiles = request.getImages().size();
        long totalFileSize = 0;

        job.sendProgress(ProgressDto.of(progressId, Category.download, "Starting download of " + totalFiles + " files", 0, totalFiles));

        for (int i = 0; i < totalFiles; i++) {
            ImageDto image = request.getImages().get(i);
            String url = image.getImageUrl();
            CropSettingsDto cropSettings = image.getCropSettings();
            if (url == null) {
                continue;
            }

            String filePath = null;
            String sourceUrl = null;
            String message;
            if (url.startsWith("/api/files/download/")) {
                filePath = FileUploadService.IMG_TEMP_FOLDER + url.replace("/api/files/download/", "");
                message = "Added uploaded file " + (i + 1) + " of " + totalFiles;
            } else {
                try {
                    filePath = fileDownloadService.downloadFile(url, hashService.curFileName(url));
                    sourceUrl = url;
                    message = "Downloaded file " + (i + 1) + " of " + totalFiles;
                } catch (Exception e) {
                    message = "Failed to download file " + (i + 1) + ": " + e.getMessage();
                }
            }

            if (filePath != null) {
                File f = new File(filePath);
                if (f.exists()) {
                    totalFileSize += f.length();
                }
                ImageDto processedImage = new ImageDto();
                processedImage.setImageUrl(filePath);
                processedImage.setCropSettings(cropSettings);
                processedImage.setSourceUrl(sourceUrl);
                processedImages.add(processedImage);
            }

            ProgressDto dto = ProgressDto.of(progressId, Category.download, message, i + 1, totalFiles);
            dto.setFileSize(totalFileSize);
            job.sendProgress(dto);

            // Im PDF wird jede Karte als PNG eingebettet und damit eher größer als die Quelldatei:
            // liegen schon die Downloads über der Grenze, lohnt das Bauen nicht
            pdfSizeLimit.check(totalFileSize, i + 1, totalFiles);
        }

        job.sendProgress(ProgressDto.of(progressId, Category.build, "Starting PDF compilation", 0, processedImages.size()));

        byte[] pdfData = latexDocumentGenerator.generateDocument(processedImages, request.getSettings(), progressId, job::sendProgress);

        job.sendProgress(ProgressDto.of(progressId, Category.finishing, "Saving PDF", 0, 1));
        pdfCacheService.addToCache(progressId, pdfData);

        ProgressDto done = ProgressDto.of(progressId, Category.finishing, "PDF compilation completed", 1, 1);
        done.setFileSize(pdfData.length);
        job.sendProgress(done);
    }
}
