package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;

public class PdfJob {
    final String progressId;
    final CreatePdfDto request;
    final List<ProgressDto> progressHistory = new CopyOnWriteArrayList<>();
    /** Adressen, die benachrichtigt werden, wenn der Job fertig ist (auch von späteren, identischen Requests). */
    final Set<String> emailRecipients = new CopyOnWriteArraySet<>();
    volatile JobStatus status = JobStatus.QUEUED;
    volatile long finishedAt;

    PdfJob(String progressId, CreatePdfDto request) {
        this.progressId = progressId;
        this.request = request;
    }

    void setStatus(JobStatus status) {
        this.status = status;
        if (status == JobStatus.FINISHED) {
            finishedAt = System.currentTimeMillis();
        }
    }

    void sendProgress(ProgressDto dto) {
        progressHistory.add(dto);
    }
}
