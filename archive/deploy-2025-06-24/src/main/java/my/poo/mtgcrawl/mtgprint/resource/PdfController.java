/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api"})
public class PdfController {
    private final PdfCacheService pdfCacheService;
    private final PDFGenerationManagerService pdfGenerationManagerService;

    public PdfController(PdfCacheService pdfCacheService, PDFGenerationManagerService pdfGenerationManagerService) {
        this.pdfGenerationManagerService = pdfGenerationManagerService;
        this.pdfCacheService = pdfCacheService;
    }

    @CrossOrigin
    @PostMapping(value={"/pdf"})
    public ResponseEntity<String> startPdfGeneration(@RequestBody CreatePdfDto request) {
        String progressId = this.pdfGenerationManagerService.submitJob(request);
        return ResponseEntity.ok(progressId);
    }

    @GetMapping(value={"/progress/{id}"})
    public List<ProgressDto> getProgress(@PathVariable String id) {
        return this.pdfGenerationManagerService.getProgressHistory(id);
    }

    @GetMapping(value={"/pdf/{progressId}"})
    public ResponseEntity<byte[]> getPdf(@PathVariable String progressId) {
        byte[] pdfContent = this.pdfCacheService.getFromCache(progressId);
        if (pdfContent == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "deck.pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<byte[]>(pdfContent, (MultiValueMap<String, String>)headers, HttpStatus.OK);
    }
}

