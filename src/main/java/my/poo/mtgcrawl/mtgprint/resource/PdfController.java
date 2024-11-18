package my.poo.mtgcrawl.mtgprint.resource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class PdfController {

    private final PDFGenerationService libTexService;
    private final Map<String, byte[]> pdfCache = new ConcurrentHashMap<>();

    public PdfController(PDFGenerationService libTexService) {
        this.libTexService = libTexService;
    }

    @CrossOrigin
    @PostMapping("/pdf")
    public ResponseEntity<String> startPdfGeneration(@RequestBody CreatePdfDto request) {
        String progressId = UUID.randomUUID().toString();
        CompletableFuture.runAsync(() -> {
            try {
                byte[] pdfContent = libTexService.generateDocument(request, progressId);
                pdfCache.put(progressId, pdfContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return ResponseEntity.ok(progressId);
    }

    @GetMapping("/pdf/{progressId}")
    public ResponseEntity<byte[]> getPdf(@PathVariable String progressId) {
        byte[] pdfContent = pdfCache.get(progressId);
        if (pdfContent == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "deck.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        pdfCache.remove(progressId); // Clean up cache

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    @GetMapping("/progress/{id}")
    public SseEmitter getProgress(@PathVariable String id) {
        return libTexService.createSseEmitter(id);
    }
}