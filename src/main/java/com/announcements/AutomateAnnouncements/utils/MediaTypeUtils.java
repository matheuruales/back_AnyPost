package com.announcements.AutomateAnnouncements.utils;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility class to detect media types from files
 */
public class MediaTypeUtils {

    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg"};
    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mkv", ".m4v"};

    /**
     * Detects if a file is an image based on its content type or extension
     */
    public static boolean isImage(MultipartFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            contentType = contentType.toLowerCase();
            if (contentType.startsWith("image/")) {
                return true;
            }
        }

        String filename = file.getOriginalFilename();
        if (StringUtils.hasText(filename)) {
            filename = filename.toLowerCase();
            for (String ext : IMAGE_EXTENSIONS) {
                if (filename.endsWith(ext)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Detects if a file is a video based on its content type or extension
     */
    public static boolean isVideo(MultipartFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            contentType = contentType.toLowerCase();
            if (contentType.startsWith("video/")) {
                return true;
            }
        }

        String filename = file.getOriginalFilename();
        if (StringUtils.hasText(filename)) {
            filename = filename.toLowerCase();
            for (String ext : VIDEO_EXTENSIONS) {
                if (filename.endsWith(ext)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines the asset type: "image" or "video"
     * Defaults to "video" if cannot be determined
     */
    public static String detectAssetType(MultipartFile file) {
        if (isImage(file)) {
            return "image";
        } else if (isVideo(file)) {
            return "video";
        }
        // Default to video for backwards compatibility
        return "video";
    }
}

