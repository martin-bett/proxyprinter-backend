/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.service.pdf.JobStatus;

public class PdfJob {
    final String progressId;
    final CreatePdfDto request;
    public final List<ProgressDto> progressHistory = new CopyOnWriteArrayList<ProgressDto>();
    volatile JobStatus status = JobStatus.QUEUED;

    PdfJob(String progressId, CreatePdfDto request) {
        this.progressId = progressId;
        this.request = request;
    }

    void setStatus(JobStatus status) {
        this.status = status;
    }

    void sendProgress(ProgressDto dto) {
        this.progressHistory.add(dto);
    }
}

