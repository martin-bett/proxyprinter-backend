package my.poo.mtgcrawl.mtgprint.service.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import my.poo.mtgcrawl.mtgprint.service.HashService;

class FileUploadServiceTest {

    @Test
    void touchOnlyWorksInsideTheUploadFolder() {
        FileUploadService service = new FileUploadService(mock(HashService.class), 7);

        assertFalse(service.touch("does-not-exist.png"));
        assertFalse(service.touch("../pom.xml"));
        assertFalse(service.touch("../../pom.xml"));
    }
}
