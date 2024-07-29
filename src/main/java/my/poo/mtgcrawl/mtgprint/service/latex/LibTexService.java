package my.poo.mtgcrawl.mtgprint.service.latex;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import my.poo.mtgcrawl.mtgprint.entitiy.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LibTexService {

    private final LatexDocumentGenerator latexDocumentGenerator;
    private final FileDownloadService fileDownloadService;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public LibTexService(LatexDocumentGenerator latexDocumentGenerator, FileDownloadService fileDownloadService) {
        this.latexDocumentGenerator = latexDocumentGenerator;
        this.fileDownloadService = fileDownloadService;
    }
    public byte[] generateDocument(CreatePdfDto request, String progressId) throws Exception {
        try {
            List<String> downloadedFileNames = new ArrayList<>();
            int totalFiles = request.getImageUrls().size();
            
            for (int i = 0; i < totalFiles; i++) {
                String url = request.getImageUrls().get(i);
                if(url != null){
                    String fileName = fileDownloadService.downloadFile(url, curFileName(url));
                    downloadedFileNames.add(fileName);
                    sendProgress(progressId, "Downloaded file " + (i + 1) + " of " + totalFiles);
                }
            }
            
            sendProgress(progressId, "Starting PDF compilation");
            
            byte[] pdfData = latexDocumentGenerator.generateDocument(downloadedFileNames, request.getSettings());
            
            sendProgress(progressId, "PDF compilation completed");
            
            return pdfData;
        } catch (Exception e) {
            sendProgress(progressId, "Error: " + e.getMessage());
            throw e;
        }
    }

   private String curFileName(String url) {
        if(url == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
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
        File f = new File(LatexDocumentGenerator.TEMP_PATH  +"/"+ progressId+ "/");
        if(f.isDirectory()){
            f.delete();
        }
    }
}