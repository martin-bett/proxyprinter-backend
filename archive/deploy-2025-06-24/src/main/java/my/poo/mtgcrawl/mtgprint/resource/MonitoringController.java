/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.resource;

import my.poo.mtgcrawl.mtgprint.entitiy.MonitoringDto;
import my.poo.mtgcrawl.mtgprint.service.rest.MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api"})
public class MonitoringController {
    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping(value={"/monitoring"})
    public MonitoringDto getMonitoringData() {
        return this.monitoringService.getMemoryMetrics();
    }
}

