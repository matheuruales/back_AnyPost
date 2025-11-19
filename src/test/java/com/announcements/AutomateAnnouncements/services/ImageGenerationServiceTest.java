package com.announcements.AutomateAnnouncements.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import com.announcements.AutomateAnnouncements.dtos.request.ImageGenerationRequestDTO;
import com.announcements.AutomateAnnouncements.dtos.response.ImageGenerationResponseDTO;
import com.announcements.AutomateAnnouncements.integration.BlobStorageService;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class ImageGenerationServiceTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void generateImageStripsQuotesFromConfigValues() throws Exception {
        String baseUrl = mockWebServer.url("/v1").toString();
        BlobStorageService blobStorageService = mock(BlobStorageService.class);
        ImageGenerationService service = new ImageGenerationService(
                WebClient.builder(),
                "  \"sk-test\"  ",
                " '" + baseUrl + "' ",
                "\"gpt-image-1\"",
                blobStorageService);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "data": [
                            {
                              "url": "https://cdn.example.com/img.png",
                              "revised_prompt": "Bike cat"
                            }
                          ]
                        }
                        """));

        ImageGenerationRequestDTO request = new ImageGenerationRequestDTO();
        request.setPrompt("Bike cat");

        ImageGenerationResponseDTO response = service.generateImage(request);

        assertThat(response.getImageUrl()).isEqualTo("https://cdn.example.com/img.png");
        assertThat(response.getRevisedPrompt()).isEqualTo("Bike cat");

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/images/generations");
        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer sk-test");
        assertThat(recordedRequest.getBody().readUtf8()).contains("\"model\":\"gpt-image-1\"");
    }
}
