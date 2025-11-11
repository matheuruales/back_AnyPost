package com.announcements.AutomateAnnouncements.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class SoraIntegrationService {

    public File generateVideoFromPrompt(String prompt) {
        log.info("Mock generating video from prompt: {}", prompt);

        // Simulate generation delay
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Return mock file
        File mockFile = new File("src/main/resources/fake_videos/generated.mp4");
        log.info("Video generated successfully. File: {}", mockFile.getAbsolutePath());

        return mockFile;
    }
}