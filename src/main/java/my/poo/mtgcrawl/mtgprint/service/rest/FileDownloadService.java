package my.poo.mtgcrawl.mtgprint.service.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FileDownloadService {
    private final RestTemplate restTemplate;
    private static final String IMG_FOLDER = "img/";
    private final Tika tika = new Tika();

    public FileDownloadService(RestTemplate r) {
        restTemplate = r;
    }

    public String downloadFile(String url, String fileName) {
        if (fileName == null) return null;
    
        // Suche zuerst nach der Datei ohne Erweiterung
        Path baseFilePath = Paths.get(IMG_FOLDER + fileName);
        try {
            Optional<Path> existingFile = Files.list(baseFilePath.getParent())
                .filter(path -> path.getFileName().toString().startsWith(fileName + "."))
                .findFirst();
    
            if (existingFile.isPresent()) {
                System.out.println("File already exists: " + existingFile.get());
                return existingFile.get().toString();
            }
        } catch (IOException e) {
            System.err.println("Error checking for existing file: " + e.getMessage());
        }
    
        // Wenn die Datei nicht existiert, fahren Sie mit dem Download fort
        String finalUrl = followRedirects(url);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<byte[]> entity = new HttpEntity<>(headers);
    
        ResponseEntity<byte[]> response = restTemplate.exchange(finalUrl, HttpMethod.GET, entity, byte[].class);
    
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                byte[] fileContent = response.getBody();
                // Detect MIME type
                String mimeType = tika.detect(fileContent);
                String extension = MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
    
                // Create file name with correct extension
                String fullFileName = IMG_FOLDER + fileName + extension;
                Path filePath = Paths.get(fullFileName);
    
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, fileContent);
                System.out.println("File downloaded successfully: " + fullFileName);
                return fullFileName;
            } catch (Exception e) {
                System.err.println("Error writing file: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            System.out.println("Failed to download file. Status code: " + response.getStatusCode());
            return null;
        }
    }

    private String followRedirects(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    
        if (response.getStatusCode().is3xxRedirection()) {
            String newUrl = response.getHeaders().getLocation().toString();
            return followRedirects(newUrl);
        }
    
        return url;
    }
}
