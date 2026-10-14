package my.poo.mtgcrawl.mtgprint.service.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/** Zählstellen für proxymaster.usage_stats; schreibt nur Summen pro Tag, siehe {@link UsageMetric}. */
@Service
public class UsageTracker {

    private final UsageStatsRepository repository;
    private final VisitorCounter visitors;

    @Autowired
    public UsageTracker(UsageStatsRepository repository) {
        this(repository, new VisitorCounter(UsageStatsRepository::today));
    }

    UsageTracker(UsageStatsRepository repository, VisitorCounter visitors) {
        this.repository = repository;
        this.visitors = visitors;
    }

    /** Ein Laden der Seite; zählt zusätzlich einen Besucher, wenn dieser heute noch nicht da war. */
    public void pageView(HttpServletRequest request) {
        repository.count(UsageMetric.PAGE_VIEWS);
        if (visitors.firstVisitToday(clientAddress(request), request.getHeader(HttpHeaders.USER_AGENT))) {
            repository.count(UsageMetric.VISITORS);
        }
    }

    public void count(UsageMetric metric) {
        repository.count(metric);
    }

    public void count(UsageMetric metric, long amount) {
        repository.count(metric, amount);
    }

    /** Hinter nginx (proxy-headers.conf) steht die echte Adresse in X-Real-IP, sonst die der Verbindung. */
    static String clientAddress(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real.trim() : request.getRemoteAddr();
    }
}
