package my.poo.mtgcrawl.mtgprint.service.pdf;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PDFGenerationService {

    private final DocumentGenerator latexDocumentGenerator;
    private final FileDownloadService fileDownloadService;
    private final HashService hashService;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public PDFGenerationService(DocumentGenerator latexDocumentGenerator, FileDownloadService fileDownloadService, HashService hashService) {
        this.latexDocumentGenerator = latexDocumentGenerator;
        this.fileDownloadService = fileDownloadService;
        this.hashService = hashService;
    }
    public byte[] generateDocument(CreatePdfDto request, String progressId) throws Exception {
    try {
        List<ImageDto> processedImages = new ArrayList<>();
        int totalFiles = request.getImages().size();
        sendProgress(progressId, "Starting download of " + totalFiles + " files");
        
        for (int i = 0; i < totalFiles; i++) {
            ImageDto image = request.getImages().get(i);
            String url = image.getImageUrl();
            CropSettingsDto cropSettings = image.getCropSettings();
            
            if (url != null) {
                String filePath = null;
                if (url.startsWith("/api/files/download/")) {
                    filePath = FileUploadService.IMG_TEMP_FOLDER + url.replace("/api/files/download/", "");
                    sendProgress(progressId, "Added uploaded file " + (i + 1) + " of " + totalFiles);
                } else {
                    try {
                        filePath = fileDownloadService.downloadFile(url, hashService.curFileName(url));
                        sendProgress(progressId, "Downloaded file " + (i + 1) + " of " + totalFiles);
                    } catch (Exception e) {
                        sendProgress(progressId, "Failed to download file " + (i + 1) + ": " + e.getMessage());
                    }
                }

                if (filePath != null) {
                    // Füge den Dateipfad und die Crop-Settings zum DTO hinzu
                    ImageDto processedImage = new ImageDto();
                    processedImage.setImageUrl(filePath);
                    processedImage.setCropSettings(cropSettings);
                    processedImages.add(processedImage);
                }
            }
        }
        
        sendProgress(progressId, "Starting PDF compilation");
        
        // Übergeben der verarbeiteten Bilder an den PDF-Generator
        byte[] pdfData = latexDocumentGenerator.generateDocument(processedImages, request.getSettings());
        
        sendProgress(progressId, "PDF compilation completed");
        
        return pdfData;
    } catch (Exception e) {
        sendProgress(progressId, "Error: " + e.getMessage());
        throw e;
    }
}



    public SseEmitter createSseEmitter(String id) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(id, emitter);
        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        return emitter;
    }

    private void sendProgress(String id, String message) {
        SseEmitter emitter = emitters.get(id);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                emitters.remove(id);
            }
        }
    }

    public void clearFiles(String progressId) {
        File f = new File(DocumentGenerator.TEMP_PATH  +"/"+ progressId+ "/");
        if(f.isDirectory()){
            f.delete();
        }
    }
}