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
import jakarta.servlet.http.HttpServletRequest;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthService;
import my.poo.mtgcrawl.mtgprint.service.mail.MailService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfSizeLimit;

@RestController
@RequestMapping("/api")
public class PdfController {

    private final PDFGenerationManagerService pdfGenerationManagerService;
    private final PdfCacheService pdfCacheService;
    private final PdfSizeLimit pdfSizeLimit;
    private final MailService mailService;
    private final AuthService authService;

    public PdfController(PDFGenerationManagerService pdfGenerationManagerService, PdfCacheService pdfCacheService,
            PdfSizeLimit pdfSizeLimit, MailService mailService, AuthService authService) {
        this.authService = authService;
        this.pdfGenerationManagerService = pdfGenerationManagerService;
        this.pdfCacheService = pdfCacheService;
        this.pdfSizeLimit = pdfSizeLimit;
        this.mailService = mailService;
    }

    /** Grenzen und Optionen, die das Frontend vor dem Anfordern prüft. */
    @GetMapping("/pdf/limits")
    public Map<String, Object> getLimits() {
        return Map.of("maxSizeMb", pdfSizeLimit.getMaxSizeMb(), "mailEnabled", mailService.isEnabled());
    }

    /**
     * Mit {@code sendMail: true} geht der Download-Link an die Adresse des angemeldeten Kontos.
     * Eine vom Client gesendete Adresse wird ignoriert, so kann niemand fremden Leuten Mails schicken lassen.
     */
    @CrossOrigin
    @PostMapping("/pdf")
    public ResponseEntity<String> startPdfGeneration(@RequestBody CreatePdfDto request, HttpServletRequest httpRequest) {
        request.setEmail(null);
        if (Boolean.TRUE.equals(request.getSendMail())) {
            AppUser user = AuthController.currentUser(authService, httpRequest).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Log in to get the PDF by mail");
            }
            request.setEmail(user.email());
        }
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

    /**
     * Liefert das fertige PDF. 202 heißt „wird noch erzeugt“, 404 „unbekannt oder schon gelöscht“.
     * Mit ?name= wird der Download-Dateiname gesetzt; der Name gehört nicht zum Request-Hash.
     */
    @GetMapping("/pdf/{progressId}")
    public ResponseEntity<byte[]> getPdf(@PathVariable String progressId, @RequestParam(required = false) String name) {
        byte[] pdfContent = pdfCacheService.getFromCache(progressId);
        if (pdfContent == null) {
            if (pdfGenerationManagerService.isInProgress(progressId)) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).build();
            }
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", downloadFileName(name));
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    /** Größe und Ablaufdatum eines gecachten PDFs, damit das Frontend „noch bis …“ anzeigen kann. */
    @GetMapping("/pdf/{progressId}/info")
    public ResponseEntity<Map<String, Object>> getPdfInfo(@PathVariable String progressId) {
        return pdfCacheService.getInfo(progressId)
                .map(info -> ResponseEntity.ok(Map.<String, Object>of(
                        "fileSize", info.fileSize(),
                        "createdAt", info.createdAt(),
                        "expiresAt", info.expiresAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    static String downloadFileName(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _.-]", "").trim().replace(' ', '-');
        cleaned = cleaned.replaceAll("^[.-]+", "");
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80);
        }
        if (cleaned.toLowerCase().endsWith(".pdf")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        return (cleaned.isEmpty() ? "deck" : cleaned) + ".pdf";
    }
}
