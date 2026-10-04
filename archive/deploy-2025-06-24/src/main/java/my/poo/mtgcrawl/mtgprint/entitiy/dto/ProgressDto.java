/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

public class ProgressDto {
    private String progressId;
    private Category category;
    private String message = "";
    private int current;
    private int total;
    private Integer pageCount;
    private int fileSize;

    public String getProgressId() {
        return this.progressId;
    }

    public Category getCategory() {
        return this.category;
    }

    public String getMessage() {
        return this.message;
    }

    public int getCurrent() {
        return this.current;
    }

    public int getTotal() {
        return this.total;
    }

    public Integer getPageCount() {
        return this.pageCount;
    }

    public int getFileSize() {
        return this.fileSize;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ProgressDto)) {
            return false;
        }
        ProgressDto other = (ProgressDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getCurrent() != other.getCurrent()) {
            return false;
        }
        if (this.getTotal() != other.getTotal()) {
            return false;
        }
        if (this.getFileSize() != other.getFileSize()) {
            return false;
        }
        Integer this$pageCount = this.getPageCount();
        Integer other$pageCount = other.getPageCount();
        if (this$pageCount == null ? other$pageCount != null : !((Object)this$pageCount).equals(other$pageCount)) {
            return false;
        }
        String this$progressId = this.getProgressId();
        String other$progressId = other.getProgressId();
        if (this$progressId == null ? other$progressId != null : !this$progressId.equals(other$progressId)) {
            return false;
        }
        Category this$category = this.getCategory();
        Category other$category = other.getCategory();
        if (this$category == null ? other$category != null : !((Object)((Object)this$category)).equals((Object)other$category)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        return !(this$message == null ? other$message != null : !this$message.equals(other$message));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ProgressDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getCurrent();
        result = result * 59 + this.getTotal();
        result = result * 59 + this.getFileSize();
        Integer $pageCount = this.getPageCount();
        result = result * 59 + ($pageCount == null ? 43 : ((Object)$pageCount).hashCode());
        String $progressId = this.getProgressId();
        result = result * 59 + ($progressId == null ? 43 : $progressId.hashCode());
        Category $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : ((Object)((Object)$category)).hashCode());
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    public String toString() {
        return "ProgressDto(progressId=" + this.getProgressId() + ", category=" + String.valueOf((Object)this.getCategory()) + ", message=" + this.getMessage() + ", current=" + this.getCurrent() + ", total=" + this.getTotal() + ", pageCount=" + this.getPageCount() + ", fileSize=" + this.getFileSize() + ")";
    }

    public static enum Category {
        download,
        build,
        queued,
        finishing;

    }
}

