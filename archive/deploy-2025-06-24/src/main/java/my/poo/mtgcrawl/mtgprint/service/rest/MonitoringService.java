/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest;

import io.micrometer.core.instrument.MeterRegistry;
import my.poo.mtgcrawl.mtgprint.entitiy.MonitoringDto;
import my.poo.mtgcrawl.mtgprint.service.pdf.BufferedImageCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringService {
    private final MeterRegistry meterRegistry;
    private final PdfCacheService pdfCacheService;
    private final BufferedImageCacheService bufferedImageCacheService;
    private final FileDownloadService fileDownloadService;
    private final PDFGenerationManagerService pdfGenerationManagerService;

    @Autowired
    public MonitoringService(MeterRegistry meterRegistry, PdfCacheService pdfCacheService, BufferedImageCacheService bufferedImageCacheService, FileDownloadService fileDownloadService, PDFGenerationManagerService pdfGenerationManagerService) {
        this.meterRegistry = meterRegistry;
        this.pdfCacheService = pdfCacheService;
        this.bufferedImageCacheService = bufferedImageCacheService;
        this.fileDownloadService = fileDownloadService;
        this.pdfGenerationManagerService = pdfGenerationManagerService;
    }

    public MonitoringDto getMemoryMetrics() {
        MonitoringDto monitoringDto = new MonitoringDto();
        monitoringDto.setMemoryMax(this.getMetric("jvm.memory.max") != null ? (Double)this.getMetric("jvm.memory.max") : 0.0);
        monitoringDto.setMemoryUsed(this.getMetric("jvm.memory.used") != null ? (Double)this.getMetric("jvm.memory.used") : 0.0);
        monitoringDto.setMemoryCommitted(this.getMetric("jvm.memory.committed") != null ? (Double)this.getMetric("jvm.memory.committed") : 0.0);
        monitoringDto.setCachedImageCount(this.fileDownloadService.getCachedImageCount());
        monitoringDto.setCachedPdfs(this.pdfCacheService.getCachedPdfCount());
        monitoringDto.setMemoryImageCount(this.bufferedImageCacheService.getMemoryImageCount());
        monitoringDto.setMemoryImageSize(this.bufferedImageCacheService.getMemoryImageSize());
        monitoringDto.setPdfQueueSize(this.pdfGenerationManagerService.getQueueSize());
        monitoringDto.setPdfActiveJobs(this.pdfGenerationManagerService.getActiveJobCount());
        monitoringDto.setPdfMaxConcurrent(this.pdfGenerationManagerService.getMaxConcurrentJobs());
        return monitoringDto;
    }

    public Object getMetric(String metricName) {
        return this.meterRegistry.find(metricName).gauge() != null ? Double.valueOf(this.meterRegistry.find(metricName).gauge().value()) : null;
    }
}

