package dev.rota.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the web demo (Next.js, http://localhost:3000 by default) call the API from the browser.
 * Only {@code /api/**}, only GET/POST, only the configured web origin ({@code rota.web.origin};
 * a comma-separated list is also accepted). The {@code Location} header is exposed so the page
 * can read the stored roster's URL. P08 renamed the property from {@code rota.cors.allowed-origins}.
 */
@Configuration(proxyBeanMethods = false)
public class WebCorsConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebCorsConfiguration(@Value("${rota.web.origin:http://localhost:3000}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins.clone();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST")
                .exposedHeaders("Location");
    }
}
