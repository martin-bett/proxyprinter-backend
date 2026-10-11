package my.poo.mtgcrawl.mtgprint.service.admin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;

/**
 * Laufzeitwerte des Backends für die Admin-Seite, ähnlich Actuator health/metrics, aber nur für Admins
 * (Actuator selbst ist nach außen gesperrt). Zeitangaben in ms, Speicher in Bytes.
 */
@Service
public class SystemStatusService {

    private final BooleanSupplier databaseUp;
    private final PDFGenerationManagerService pdfJobs;

    @Autowired
    public SystemStatusService(JdbcTemplate jdbc, PDFGenerationManagerService pdfJobs) {
        this(() -> {
            try {
                return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class));
            } catch (RuntimeException e) {
                return false;
            }
        }, pdfJobs);
    }

    SystemStatusService(BooleanSupplier databaseUp, PDFGenerationManagerService pdfJobs) {
        this.databaseUp = databaseUp;
        this.pdfJobs = pdfJobs;
    }

    public Map<String, Object> status() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        File workDir = new File(".").getAbsoluteFile();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("database", databaseUp.getAsBoolean() ? "UP" : "DOWN");
        result.put("startedAt", runtime.getStartTime());
        result.put("uptimeMs", runtime.getUptime());
        result.put("javaVersion", Runtime.version().toString());
        result.put("processors", Runtime.getRuntime().availableProcessors());
        result.put("systemLoad", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        result.put("heapUsed", heap.getUsed());
        result.put("heapCommitted", heap.getCommitted());
        result.put("heapMax", heap.getMax());
        result.put("nonHeapUsed", nonHeap.getUsed());
        result.put("threads", threads.getThreadCount());
        result.put("diskFree", workDir.getUsableSpace());
        result.put("diskTotal", workDir.getTotalSpace());
        result.put("pdfJobsActive", pdfJobs.getActiveJobCount());
        result.put("pdfJobsQueued", pdfJobs.getQueueSize());
        result.put("pdfJobsMax", pdfJobs.getMaxConcurrentJobs());
        return result;
    }
}
