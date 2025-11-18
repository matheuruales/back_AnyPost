package com.announcements.AutomateAnnouncements.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS = List.of("*");
    private static final List<String> EXPOSED_HEADERS = List.of("Location");

    private final List<String> explicitOrigins;
    private final List<String> originPatterns;
    private final boolean wildcardMode;
    private final boolean allowCredentials;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:*}") String allowedOriginsProperty,
            @Value("${app.cors.allow-credentials:true}") boolean allowCredentials) {
        var trimmed = allowedOriginsProperty.trim();
        this.allowCredentials = allowCredentials;

        if ("*".equals(trimmed)) {
            this.wildcardMode = true;
            this.explicitOrigins = List.of();
            this.originPatterns = List.of("*");
            return;
        }

        var origins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        if (origins.isEmpty()) {
            this.wildcardMode = true;
            this.explicitOrigins = List.of();
            this.originPatterns = List.of("*");
            return;
        }

        this.wildcardMode = origins.contains("*");
        this.explicitOrigins = origins.stream()
                .filter(origin -> !origin.contains("*"))
                .toList();
        this.originPatterns = origins.stream()
                .filter(origin -> origin.contains("*"))
                .map(origin -> "*".equals(origin) ? "*" : origin)
                .toList();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Auth endpoints: open to any origin (no credentials) so password recovery works even if CORS env is misconfigured.
                registry.addMapping("/api/auth/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods(ALLOWED_METHODS.toArray(new String[0]))
                        .allowedHeaders(ALLOWED_HEADERS.toArray(new String[0]))
                        .exposedHeaders(EXPOSED_HEADERS.toArray(new String[0]))
                        .allowCredentials(false);

                var mapping = registry.addMapping("/api/**")
                        .allowedMethods(ALLOWED_METHODS.toArray(new String[0]))
                        .allowedHeaders(ALLOWED_HEADERS.toArray(new String[0]))
                        .exposedHeaders(EXPOSED_HEADERS.toArray(new String[0]));

                applyOriginRules(mapping);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", createCorsConfiguration());
        source.registerCorsConfiguration("/**", createCorsConfiguration());
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsConfigurationSource corsConfigurationSource) {
        var filterRegistration = new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource));
        filterRegistration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistration;
    }

    private void applyOriginRules(CorsRegistration registration) {
        if (wildcardMode) {
            registration.allowedOriginPatterns("*").allowCredentials(false);
            return;
        }

        if (!explicitOrigins.isEmpty()) {
            registration.allowedOrigins(explicitOrigins.toArray(new String[0]));
        }
        if (!originPatterns.isEmpty()) {
            registration.allowedOriginPatterns(originPatterns.toArray(new String[0]));
        }

        registration.allowCredentials(allowCredentials);
    }

    private CorsConfiguration createCorsConfiguration() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);

        if (wildcardMode) {
            configuration.addAllowedOriginPattern("*");
            configuration.setAllowCredentials(false);
        } else {
            explicitOrigins.forEach(configuration::addAllowedOrigin);
            originPatterns.forEach(configuration::addAllowedOriginPattern);
            configuration.setAllowCredentials(allowCredentials);
        }

        return configuration;
    }
}
