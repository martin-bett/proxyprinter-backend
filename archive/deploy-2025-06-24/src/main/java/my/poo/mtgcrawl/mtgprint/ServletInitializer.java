/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint;

import my.poo.mtgcrawl.mtgprint.MtgprintApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer
extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MtgprintApplication.class);
    }
}

