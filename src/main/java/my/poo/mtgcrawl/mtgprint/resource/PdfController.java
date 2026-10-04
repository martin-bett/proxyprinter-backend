package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfSizeLimit;

@RestController
@RequestMapping("/api")
public class PdfController {

    private final PDFGenerationManagerService pdfGenerationManagerService;
    private final PdfCacheService pdfCacheService;
    private final PdfSizeLimit pdfSizeLimit;

    public PdfController(PDFGenerationManagerService pdfGenerationManagerService, PdfCacheService pdfCacheService,
            PdfSizeLimit pdfSizeLimit) {
        this.pdfGenerationManagerService = pdfGenerationManagerService;
        this.pdfCacheService = pdfCacheService;
        this.pdfSizeLimit = pdfSizeLimit;
    }

    /** Grenzen, die das Frontend vor dem Anfordern prüft. */
    @GetMapping("/pdf/limits")
    public Map<String, Long> getLimits() {
        return Map.of("maxSizeMb", pdfSizeLimit.getMaxSizeMb());
    }

    @CrossOrigin
    @PostMapping("/pdf")
    public ResponseEntity<String> startPdfGeneration(@RequestBody CreatePdfDto request) {
        return ResponseEntity.ok(pdfGenerationManagerService.submitJob(request));
    }

    @GetMapping("/progress/{id}")
    public ResponseEntity<List<ProgressDto>> getProgress(@PathVariable String id) {
        List<ProgressDto> history = pdfGenerationManagerService.getProgressHistory(id);
        if (history == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(history);
    }

    @GetMapping("/pdf/{progressId}")
    public ResponseEntity<byte[]> getPdf(@PathVariable String progressId) {
        byte[] pdfContent = pdfCacheService.getFromCache(progressId);
        if (pdfContent == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "deck.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
