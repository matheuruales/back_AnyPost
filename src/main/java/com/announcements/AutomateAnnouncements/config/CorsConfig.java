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

    private final List<String> allowedOrigins;
    private final boolean allowCredentials;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:*}") String allowedOriginsProperty,
            @Value("${app.cors.allow-credentials:false}") boolean allowCredentials) {
        this.allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        this.allowCredentials = allowCredentials;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(allowedOrigins.isEmpty() ? new String[]{"*"} : allowedOrigins.toArray(new String[0]))
                        .allowedMethods(ALLOWED_METHODS.toArray(new String[0]))
                        .allowedHeaders(ALLOWED_HEADERS.toArray(new String[0]))
                        .exposedHeaders(EXPOSED_HEADERS.toArray(new String[0]))
                        .allowCredentials(allowCredentials);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var source = new UrlBasedCorsConfigurationSource();
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
        // Deprecated helper; centralized in addCorsMappings
    }

    private CorsConfiguration createCorsConfiguration() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowedOriginPatterns(allowedOrigins.isEmpty() ? List.of("*") : allowedOrigins);
        configuration.setAllowCredentials(allowCredentials);

        return configuration;
    }
}
