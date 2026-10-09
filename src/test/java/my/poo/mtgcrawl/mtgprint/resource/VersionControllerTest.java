package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;

class VersionControllerTest {

    @Test
    void usesBuildNumberFromBuildInfo() {
        Properties p = new Properties();
        p.setProperty("number", "20261009-1347");
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("buildProperties", new BuildProperties(p));

        VersionController c = new VersionController(beans.getBeanProvider(BuildProperties.class));

        assertEquals("20261009-1347", c.version().get("build"));
    }

    @Test
    void fallsBackToStartTimeWithoutBuildInfo() {
        VersionController c = new VersionController(new StaticListableBeanFactory().getBeanProvider(BuildProperties.class));

        assertTrue(c.version().get("build").matches("\\d{8}-\\d{4}"));
    }
}
