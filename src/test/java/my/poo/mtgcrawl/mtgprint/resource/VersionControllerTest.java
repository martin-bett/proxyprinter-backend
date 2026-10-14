package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.web.MockHttpServletRequest;

import my.poo.mtgcrawl.mtgprint.service.stats.UsageTracker;

class VersionControllerTest {

    private final UsageTracker usage = mock(UsageTracker.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void usesBuildNumberFromBuildInfo() {
        Properties p = new Properties();
        p.setProperty("number", "20261009-1347");
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("buildProperties", new BuildProperties(p));

        VersionController c = new VersionController(beans.getBeanProvider(BuildProperties.class), usage);

        assertEquals("20261009-1347", c.version(request).get("build"));
        verify(usage).pageView(request);
    }

    @Test
    void fallsBackToStartTimeWithoutBuildInfo() {
        VersionController c = new VersionController(new StaticListableBeanFactory().getBeanProvider(BuildProperties.class), usage);

        assertTrue(c.version(request).get("build").matches("\\d{8}-\\d{4}"));
    }
}
