package dev.rota.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the P07 web demo (Next.js dev server, http://localhost:3000 by default) call the API from
 * the browser. Only {@code /api/**}, only GET/POST, only the configured origins
 * ({@code rota.cors.allowed-origins}, comma-separated). The {@code Location} header is exposed so
 * the page can read the stored roster's URL.
 */
@Configuration(proxyBeanMethods = false)
public class WebCorsConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebCorsConfiguration(@Value("${rota.cors.allowed-origins:http://localhost:3000}") String[] allowedOrigins) {
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
