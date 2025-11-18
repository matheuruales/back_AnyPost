package com.announcements.AutomateAnnouncements.dtos.request;

/**
 * Builder pattern – centralizes validation and creation of video generation parameters so
 * controllers can express intent fluently and we can evolve the contract safely over time.
 */
public class VideoGenerationRequest {

    private final String prompt;
    private final String title;
    private final String description;
    private final String targets;
    private final String style;

    private VideoGenerationRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.title = builder.title;
        this.description = builder.description;
        this.targets = builder.targets;
        this.style = builder.style;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTargets() {
        return targets;
    }

    public String getStyle() {
        return style;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String prompt;
        private String title;
        private String description;
        private String targets;
        private String style;

        private Builder() {}

        public Builder withPrompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withTargets(String targets) {
            this.targets = targets;
            return this;
        }

        public Builder withStyle(String style) {
            this.style = style;
            return this;
        }

        public VideoGenerationRequest build() {
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("Prompt is required");
            }
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (targets == null || targets.isBlank()) {
                throw new IllegalArgumentException("Targets are required");
            }
            return new VideoGenerationRequest(this);
        }
    }
}
