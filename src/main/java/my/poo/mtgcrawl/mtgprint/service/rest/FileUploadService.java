package my.poo.mtgcrawl.mtgprint.service.rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.FileUploadResult;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.exc.FileWriteException;
import my.poo.mtgcrawl.mtgprint.service.rest.exc.WrongFormatException;

@Service
@Slf4j
public class FileUploadService {
    
    public static final String IMG_TEMP_FOLDER = "img/temp/";
    private final HashService hashService;
    

    public FileUploadService(HashService hashService) {
        this.hashService = hashService;
        try {
            Files.createDirectories(Paths.get(IMG_TEMP_FOLDER));
        } catch (IOException e) {
            log.error("Konnte Upload-Ordner {} nicht anlegen", IMG_TEMP_FOLDER, e);
        }
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void clearTempFiles(){
        File[] files = new File(IMG_TEMP_FOLDER).listFiles();
        if (files == null) {
            return;
        }
        for(File f : files){
            if(new Date().getTime() - f.lastModified() >= 24 * 60 * 60 * 1000){
                f.delete();
            }                
        }
    }

    public FileUploadResult uploadFile(MultipartFile file){
        FileUploadResult fur = new FileUploadResult();
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = generateUniqueFileName(originalFilename) + fileExtension;
        fur.setGeneratedId(uniqueFileName);
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        }catch(Exception e){
            fur.setSuccess(false);
            fur.setException(new WrongFormatException(e));
        }
        try {
             Files.createDirectories(Paths.get(IMG_TEMP_FOLDER));
            File outputFile = new File(IMG_TEMP_FOLDER + uniqueFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 InputStream inputStream = new ByteArrayInputStream(file.getBytes())) {
                IOUtils.copy(inputStream, fos);
                fur.setSuccess(true);
            }
        } catch (Exception e) {
            fur.setSuccess(false);
            fur.setException(new FileWriteException(e));
        }
        return fur;
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension found
        }
        return filename.substring(lastIndexOf);
    }

        private String generateUniqueFileName(String originalFileName) {
        String baseFileName = hashService.curFileName(originalFileName);
        String uniqueID = UUID.randomUUID().toString();
        return baseFileName + "-" + uniqueID;
    }
}
