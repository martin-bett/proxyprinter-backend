/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.FileUploadResult;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/files"})
public class UploadController {
    private static final String IMG_TEMP_FOLDER = "img/temp/";
    private final FileUploadService fileUploadService;

    public UploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(value={"/upload"})
    public ResponseEntity<FileUploadResult> uploadFile(@RequestParam(value="file") MultipartFile file) {
        FileUploadResult result = this.fileUploadService.uploadFile(file);
        if (result.getException() != null) {
            switch (result.getException().getClass().getSimpleName()) {
                case "WrongFormatException": {
                    return ResponseEntity.badRequest().body(new FileUploadResult(false, "Ung\u00fcltiges Dateiformat: " + result.getException().getMessage()));
                }
                case "FileWriteException": {
                    return ResponseEntity.internalServerError().body(new FileUploadResult(false, "Fehler beim Speichern der Datei: " + result.getException().getMessage()));
                }
            }
            return ResponseEntity.internalServerError().body(new FileUploadResult(false, "Unerwarteter Fehler: " + result.getException().getMessage()));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping(value={"/download/{generatedId}"})
    public ResponseEntity<byte[]> downloadFile(@PathVariable String generatedId) {
        try {
            Path filePath = Paths.get(IMG_TEMP_FOLDER, new String[0]).resolve(generatedId).normalize();
            UrlResource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                byte[] fileContent = Files.readAllBytes(filePath);
                String contentType = Files.probeContentType(filePath);
                return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).header("Content-Disposition", "attachment; filename=\"" + generatedId + "\"")).body(fileContent);
            }
            return ResponseEntity.notFound().build();
        }
        catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

