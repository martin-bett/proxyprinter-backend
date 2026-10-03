/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

public class MonitoringDto {
    private Double memoryMax;
    private Double memoryUsed;
    private Double memoryCommitted;
    private Integer cachedImageCount;
    private Integer cachedPdfs;
    private Integer memoryImageCount;
    private Long memoryImageSize;
    private int pdfQueueSize;
    private int pdfActiveJobs;
    private int pdfMaxConcurrent;

    public Double getMemoryMax() {
        return this.memoryMax;
    }

    public Double getMemoryUsed() {
        return this.memoryUsed;
    }

    public Double getMemoryCommitted() {
        return this.memoryCommitted;
    }

    public Integer getCachedImageCount() {
        return this.cachedImageCount;
    }

    public Integer getCachedPdfs() {
        return this.cachedPdfs;
    }

    public Integer getMemoryImageCount() {
        return this.memoryImageCount;
    }

    public Long getMemoryImageSize() {
        return this.memoryImageSize;
    }

    public int getPdfQueueSize() {
        return this.pdfQueueSize;
    }

    public int getPdfActiveJobs() {
        return this.pdfActiveJobs;
    }

    public int getPdfMaxConcurrent() {
        return this.pdfMaxConcurrent;
    }

    public void setMemoryMax(Double memoryMax) {
        this.memoryMax = memoryMax;
    }

    public void setMemoryUsed(Double memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public void setMemoryCommitted(Double memoryCommitted) {
        this.memoryCommitted = memoryCommitted;
    }

    public void setCachedImageCount(Integer cachedImageCount) {
        this.cachedImageCount = cachedImageCount;
    }

    public void setCachedPdfs(Integer cachedPdfs) {
        this.cachedPdfs = cachedPdfs;
    }

    public void setMemoryImageCount(Integer memoryImageCount) {
        this.memoryImageCount = memoryImageCount;
    }

    public void setMemoryImageSize(Long memoryImageSize) {
        this.memoryImageSize = memoryImageSize;
    }

    public void setPdfQueueSize(int pdfQueueSize) {
        this.pdfQueueSize = pdfQueueSize;
    }

    public void setPdfActiveJobs(int pdfActiveJobs) {
        this.pdfActiveJobs = pdfActiveJobs;
    }

    public void setPdfMaxConcurrent(int pdfMaxConcurrent) {
        this.pdfMaxConcurrent = pdfMaxConcurrent;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MonitoringDto)) {
            return false;
        }
        MonitoringDto other = (MonitoringDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getPdfQueueSize() != other.getPdfQueueSize()) {
            return false;
        }
        if (this.getPdfActiveJobs() != other.getPdfActiveJobs()) {
            return false;
        }
        if (this.getPdfMaxConcurrent() != other.getPdfMaxConcurrent()) {
            return false;
        }
        Double this$memoryMax = this.getMemoryMax();
        Double other$memoryMax = other.getMemoryMax();
        if (this$memoryMax == null ? other$memoryMax != null : !((Object)this$memoryMax).equals(other$memoryMax)) {
            return false;
        }
        Double this$memoryUsed = this.getMemoryUsed();
        Double other$memoryUsed = other.getMemoryUsed();
        if (this$memoryUsed == null ? other$memoryUsed != null : !((Object)this$memoryUsed).equals(other$memoryUsed)) {
            return false;
        }
        Double this$memoryCommitted = this.getMemoryCommitted();
        Double other$memoryCommitted = other.getMemoryCommitted();
        if (this$memoryCommitted == null ? other$memoryCommitted != null : !((Object)this$memoryCommitted).equals(other$memoryCommitted)) {
            return false;
        }
        Integer this$cachedImageCount = this.getCachedImageCount();
        Integer other$cachedImageCount = other.getCachedImageCount();
        if (this$cachedImageCount == null ? other$cachedImageCount != null : !((Object)this$cachedImageCount).equals(other$cachedImageCount)) {
            return false;
        }
        Integer this$cachedPdfs = this.getCachedPdfs();
        Integer other$cachedPdfs = other.getCachedPdfs();
        if (this$cachedPdfs == null ? other$cachedPdfs != null : !((Object)this$cachedPdfs).equals(other$cachedPdfs)) {
            return false;
        }
        Integer this$memoryImageCount = this.getMemoryImageCount();
        Integer other$memoryImageCount = other.getMemoryImageCount();
        if (this$memoryImageCount == null ? other$memoryImageCount != null : !((Object)this$memoryImageCount).equals(other$memoryImageCount)) {
            return false;
        }
        Long this$memoryImageSize = this.getMemoryImageSize();
        Long other$memoryImageSize = other.getMemoryImageSize();
        return !(this$memoryImageSize == null ? other$memoryImageSize != null : !((Object)this$memoryImageSize).equals(other$memoryImageSize));
    }

    protected boolean canEqual(Object other) {
        return other instanceof MonitoringDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getPdfQueueSize();
        result = result * 59 + this.getPdfActiveJobs();
        result = result * 59 + this.getPdfMaxConcurrent();
        Double $memoryMax = this.getMemoryMax();
        result = result * 59 + ($memoryMax == null ? 43 : ((Object)$memoryMax).hashCode());
        Double $memoryUsed = this.getMemoryUsed();
        result = result * 59 + ($memoryUsed == null ? 43 : ((Object)$memoryUsed).hashCode());
        Double $memoryCommitted = this.getMemoryCommitted();
        result = result * 59 + ($memoryCommitted == null ? 43 : ((Object)$memoryCommitted).hashCode());
        Integer $cachedImageCount = this.getCachedImageCount();
        result = result * 59 + ($cachedImageCount == null ? 43 : ((Object)$cachedImageCount).hashCode());
        Integer $cachedPdfs = this.getCachedPdfs();
        result = result * 59 + ($cachedPdfs == null ? 43 : ((Object)$cachedPdfs).hashCode());
        Integer $memoryImageCount = this.getMemoryImageCount();
        result = result * 59 + ($memoryImageCount == null ? 43 : ((Object)$memoryImageCount).hashCode());
        Long $memoryImageSize = this.getMemoryImageSize();
        result = result * 59 + ($memoryImageSize == null ? 43 : ((Object)$memoryImageSize).hashCode());
        return result;
    }

    public String toString() {
        return "MonitoringDto(memoryMax=" + this.getMemoryMax() + ", memoryUsed=" + this.getMemoryUsed() + ", memoryCommitted=" + this.getMemoryCommitted() + ", cachedImageCount=" + this.getCachedImageCount() + ", cachedPdfs=" + this.getCachedPdfs() + ", memoryImageCount=" + this.getMemoryImageCount() + ", memoryImageSize=" + this.getMemoryImageSize() + ", pdfQueueSize=" + this.getPdfQueueSize() + ", pdfActiveJobs=" + this.getPdfActiveJobs() + ", pdfMaxConcurrent=" + this.getPdfMaxConcurrent() + ")";
    }
}

