package my.poo.mtgcrawl.mtgprint.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;

class SystemStatusServiceTest {

    @Test
    void reportsDatabaseJobsAndMemory() {
        PDFGenerationManagerService jobs = mock(PDFGenerationManagerService.class);
        when(jobs.getActiveJobCount()).thenReturn(1);
        when(jobs.getQueueSize()).thenReturn(3);
        when(jobs.getMaxConcurrentJobs()).thenReturn(2);

        Map<String, Object> up = new SystemStatusService(() -> true, jobs).status();
        assertEquals("UP", up.get("database"));
        assertEquals(1, up.get("pdfJobsActive"));
        assertEquals(3, up.get("pdfJobsQueued"));
        assertEquals(2, up.get("pdfJobsMax"));
        assertTrue((Long) up.get("heapUsed") > 0);
        assertTrue((Long) up.get("diskTotal") > 0);
        assertTrue((Integer) up.get("threads") > 0);

        assertEquals("DOWN", new SystemStatusService(() -> false, jobs).status().get("database"));
    }
}
