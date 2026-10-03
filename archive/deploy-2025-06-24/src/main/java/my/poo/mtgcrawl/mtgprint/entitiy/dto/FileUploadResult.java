/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import my.poo.mtgcrawl.mtgprint.service.rest.exc.WrapperException;

public class FileUploadResult {
    private boolean success;
    private String generatedId;
    private String message;
    private WrapperException exception;

    public FileUploadResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.generatedId = null;
    }

    public Exception getException() {
        return this.exception != null ? this.exception.getActual() : null;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getGeneratedId() {
        return this.generatedId;
    }

    public String getMessage() {
        return this.message;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setGeneratedId(String generatedId) {
        this.generatedId = generatedId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setException(WrapperException exception) {
        this.exception = exception;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FileUploadResult)) {
            return false;
        }
        FileUploadResult other = (FileUploadResult)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isSuccess() != other.isSuccess()) {
            return false;
        }
        String this$generatedId = this.getGeneratedId();
        String other$generatedId = other.getGeneratedId();
        if (this$generatedId == null ? other$generatedId != null : !this$generatedId.equals(other$generatedId)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
            return false;
        }
        Exception this$exception = this.getException();
        Exception other$exception = other.getException();
        return !(this$exception == null ? other$exception != null : !this$exception.equals(other$exception));
    }

    protected boolean canEqual(Object other) {
        return other instanceof FileUploadResult;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isSuccess() ? 79 : 97);
        String $generatedId = this.getGeneratedId();
        result = result * 59 + ($generatedId == null ? 43 : $generatedId.hashCode());
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        Exception $exception = this.getException();
        result = result * 59 + ($exception == null ? 43 : $exception.hashCode());
        return result;
    }

    public String toString() {
        return "FileUploadResult(success=" + this.isSuccess() + ", generatedId=" + this.getGeneratedId() + ", message=" + this.getMessage() + ", exception=" + String.valueOf(this.getException()) + ")";
    }

    public FileUploadResult() {
    }
}

