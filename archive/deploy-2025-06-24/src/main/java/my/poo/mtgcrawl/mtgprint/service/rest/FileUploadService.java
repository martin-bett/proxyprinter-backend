/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Date;
import java.util.UUID;
import javax.imageio.ImageIO;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.FileUploadResult;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.exc.FileWriteException;
import my.poo.mtgcrawl.mtgprint.service.rest.exc.WrongFormatException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {
    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    public static final String IMG_TEMP_FOLDER = "img/temp/";
    private final HashService hashService;

    public FileUploadService(HashService hashService) {
        this.hashService = hashService;
        try {
            Files.createDirectories(Paths.get(IMG_TEMP_FOLDER, new String[0]), new FileAttribute[0]);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate=3600000L)
    public void clearTempFiles() {
        File folder = new File(IMG_TEMP_FOLDER);
        for (File f : folder.listFiles()) {
            if (new Date().getTime() - f.lastModified() < 86400000L) continue;
            f.delete();
        }
    }

    public FileUploadResult uploadFile(MultipartFile file) {
        FileUploadResult fur = new FileUploadResult();
        String originalFilename = file.getOriginalFilename();
        String fileExtension = this.getFileExtension(originalFilename);
        String uniqueFileName = this.generateUniqueFileName(originalFilename) + fileExtension;
        fur.setGeneratedId(uniqueFileName);
        try {
            ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        }
        catch (Exception e) {
            fur.setSuccess(false);
            fur.setException(new WrongFormatException(e));
        }
        try {
            File outputFile = new File(IMG_TEMP_FOLDER + uniqueFileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes());){
                IOUtils.copy((InputStream)inputStream, (OutputStream)fos);
                fur.setSuccess(true);
            }
        }
        catch (Exception e) {
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
            return "";
        }
        return filename.substring(lastIndexOf);
    }

    private String generateUniqueFileName(String originalFileName) {
        String baseFileName = this.hashService.curFileName(originalFileName);
        String uniqueID = UUID.randomUUID().toString();
        return baseFileName + "-" + uniqueID;
    }
}

