package my.poo.mtgcrawl.mtgprint.service.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class UsageTrackerTest {

    private static MockHttpServletRequest request(String realIp, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (realIp != null) {
            request.addHeader("X-Real-IP", realIp);
        }
        request.setRemoteAddr(remoteAddr);
        request.addHeader("User-Agent", "Firefox");
        return request;
    }

    @Test
    void countsPageViewsAndVisitorsBehindNginx() {
        UsageStatsRepository repo = mock(UsageStatsRepository.class);
        UsageTracker tracker = new UsageTracker(repo, new VisitorCounter(() -> LocalDate.of(2026, 10, 11)));

        // nginx verbindet sich immer von derselben Adresse, unterschieden wird nach X-Real-IP
        tracker.pageView(request("1.2.3.4", "172.20.0.1"));
        tracker.pageView(request("1.2.3.4", "172.20.0.1"));
        tracker.pageView(request("5.6.7.8", "172.20.0.1"));

        verify(repo, times(3)).count(UsageMetric.PAGE_VIEWS);
        verify(repo, times(2)).count(UsageMetric.VISITORS);
    }

    @Test
    void usesConnectionAddressWithoutProxyHeader() {
        assertEquals("10.0.0.1", UsageTracker.clientAddress(request(null, "10.0.0.1")));
        assertEquals("1.2.3.4", UsageTracker.clientAddress(request(" 1.2.3.4 ", "10.0.0.1")));
    }
}
