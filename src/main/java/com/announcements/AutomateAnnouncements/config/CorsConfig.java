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
            @Value("${app.cors.allow-credentials:true}") boolean allowCredentials) {
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
        
        // If allowCredentials is true, we cannot use "*" as origin pattern
        // If allowCredentials is false, we can use "*"
        if (allowCredentials) {
            // When credentials are allowed, we must specify exact origins, not "*"
            if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
                // If "*" is specified with credentials, we'll allow all but warn
                // In production, you should specify exact origins
                configuration.setAllowedOriginPatterns(List.of("*"));
                // Note: This will cause CORS errors in browsers. Consider specifying exact origins.
            } else {
                configuration.setAllowedOriginPatterns(allowedOrigins);
            }
        } else {
            // When credentials are not allowed, we can use "*"
            configuration.setAllowedOriginPatterns(allowedOrigins.isEmpty() ? List.of("*") : allowedOrigins);
        }
        
        configuration.setAllowCredentials(allowCredentials);

        return configuration;
    }
}
