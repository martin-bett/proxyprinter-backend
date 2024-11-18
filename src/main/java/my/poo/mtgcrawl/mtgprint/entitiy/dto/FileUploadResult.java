package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import my.poo.mtgcrawl.mtgprint.service.rest.WrapperException;

@Data
@NoArgsConstructor
public class FileUploadResult {
        private boolean success;
        private String generatedId;
        private String message;
        private WrapperException exception;

        public FileUploadResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            generatedId = null;
        }

        public Exception getException(){
            return exception != null ? exception.getActual() : null;
        }
}
