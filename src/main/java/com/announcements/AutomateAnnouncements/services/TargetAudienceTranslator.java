package com.announcements.AutomateAnnouncements.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Bridges legacy comma-separated target strings with the normalized list-based representation.
 * Ensures targets are always trimmed and safe to persist or send to downstream services.
 */
@Component
public class TargetAudienceTranslator {

    public List<String> toAudienceList(String rawTargets) {
        if (!StringUtils.hasText(rawTargets)) {
            return new ArrayList<>();
        }
        return Arrays.stream(rawTargets.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<String> sanitizeAudienceList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        return values.stream()
                .map(value -> value != null ? value.trim() : null)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public String toCsv(List<String> values) {
        List<String> sanitized = sanitizeAudienceList(values);
        return String.join(",", sanitized);
    }
}
