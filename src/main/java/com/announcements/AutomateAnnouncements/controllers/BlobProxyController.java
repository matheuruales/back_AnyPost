package com.announcements.AutomateAnnouncements.controllers;

import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Controller
@RequestMapping("/api/videos")
public class BlobProxyController {

    @Value("${azure.blob.container-name:}")
    private String containerName;

    @GetMapping(path = "/stream")
    public ResponseEntity<StreamingResponseBody> streamBlob(
            @RequestParam("url") String blobUrl,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        try {
            URI uri = URI.create(blobUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only HTTPS blob URLs are allowed");
            }

            String host = uri.getHost();
            if (host == null || !host.endsWith(".blob.core.windows.net")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid blob host");
            }

            if (containerName != null && !containerName.isBlank()) {
                String path = uri.getPath();
                String expectedPrefix = "/" + containerName + "/";
                if (path == null || !path.startsWith(expectedPrefix)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blob URL does not belong to the configured container");
                }
            }

            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setRequestMethod("GET");

            if (rangeHeader != null && !rangeHeader.isBlank()) {
                conn.setRequestProperty("Range", rangeHeader);
            }

            // forward some common headers
            int status = conn.getResponseCode();

            HttpHeaders headers = new HttpHeaders();
            String contentType = conn.getHeaderField("Content-Type");
            if (contentType != null) {
                try {
                    headers.setContentType(MediaType.parseMediaType(contentType));
                } catch (Exception ex) {
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                }
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            String acceptRanges = conn.getHeaderField("Accept-Ranges");
            if (acceptRanges != null) {
                headers.add("Accept-Ranges", acceptRanges);
            }

            String contentLength = conn.getHeaderField("Content-Length");
            if (contentLength != null) {
                headers.add("Content-Length", contentLength);
            }

            String contentRange = conn.getHeaderField("Content-Range");
            if (contentRange != null) {
                headers.add("Content-Range", contentRange);
            }

            List<String> cors = List.of("*");
            headers.put(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, cors);

            InputStream inputStream;
            if (status >= 400) {
                inputStream = conn.getErrorStream();
            } else {
                inputStream = conn.getInputStream();
            }

            StreamingResponseBody body = (OutputStream out) -> {
                try (InputStream in = inputStream; OutputStream os = out) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        os.flush();
                    }
                } catch (Exception e) {
                    log.warn("Error streaming blob proxy: {}", e.getMessage());
                }
            };

            return ResponseEntity.status(status).headers(headers).body(body);

        } catch (Exception ex) {
            log.error("Failed to proxy blob URL {}: {}", blobUrl, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
        }
    }
}
