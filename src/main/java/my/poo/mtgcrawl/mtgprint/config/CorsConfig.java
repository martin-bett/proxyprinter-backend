package my.poo.mtgcrawl.mtgprint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:8081"); // Erlaubt Anfragen von Ihrem React-Frontend
        config.addAllowedOrigin("http://46.38.245.243:8081"); // Erlaubt Anfragen von Ihrem React-Frontend
        config.addAllowedOrigin("http://proxyprinter.eu:8081"); // Erlaubt Anfragen von Ihrem React-Frontend
        config.addAllowedOrigin("http://localhost:3000"); // Erlaubt Anfragen von Ihrem React-Frontend
        config.addAllowedOrigin("https://proxyprinter.eu"); // Live-Seite (POST mit Origin-Header, sonst 403)
        config.addAllowedOrigin("https://www.proxyprinter.eu");
        config.addAllowedOriginPattern("https://test.proxyprinter.eu"); // Testumgebungen (deploy/PREVIEW.md)
        config.addAllowedOriginPattern("https://*.test.proxyprinter.eu");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
