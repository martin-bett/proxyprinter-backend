package my.poo.mtgcrawl.mtgprint.resource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.FileUploadResult;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String IMG_TEMP_FOLDER = "img/temp/";
    private final FileUploadService fileUploadService;

    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResult result = fileUploadService.uploadFile(file);
        if(result.getException() != null){
            switch (result.getException().getClass().getSimpleName()) {
            case "WrongFormatException":
                return ResponseEntity.badRequest().body(new FileUploadResult(false, "Ungültiges Dateiformat: " + result.getException().getMessage()));
            case "FileWriteException":
                return ResponseEntity.internalServerError().body(new FileUploadResult(false, "Fehler beim Speichern der Datei: " + result.getException().getMessage()));
            default:
                return ResponseEntity.internalServerError().body(new FileUploadResult(false, "Unerwarteter Fehler: " + result.getException().getMessage()));
            }
        }
        return ResponseEntity.ok(result);
    }

    /** Verlängert die Aufbewahrung eines Uploads (für Snapshots). 204 = verlängert, 404 = schon gelöscht. */
    @PostMapping("/{generatedId}/touch")
    public ResponseEntity<Void> touchFile(@PathVariable String generatedId) {
        return fileUploadService.touch(generatedId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/download/{generatedId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String generatedId) {
        try {
            Path filePath = Paths.get(IMG_TEMP_FOLDER).resolve(generatedId).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                byte[] fileContent = Files.readAllBytes(filePath);
                String contentType = Files.probeContentType(filePath);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + generatedId + "\"")
                        .body(fileContent);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
