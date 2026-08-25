package com.aaax.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class FileUtil {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Map<String, String> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("heic", "image/heic"),
            Map.entry("heif", "image/heif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("zip", "application/zip")
    );

    public static String getFileName(MultipartFile file) {
        return Optional.ofNullable(file.getOriginalFilename()).orElse(file.getName());
    }

    public static String getFileExtension(MultipartFile file) {
        var splits = Optional.ofNullable(file.getContentType())
                .map(type -> type.split("/"))
                .orElseGet(() -> getFileName(file).split("\\."));
        return splits[splits.length - 1].toLowerCase(Locale.ROOT);
    }

    /**
     * Prefer multipart Content-Type when specific; otherwise infer from file name / URL extension.
     */
    public static String resolveMimeType(MultipartFile file) {
        String fromHeader = file.getContentType();
        if (StringUtils.isNotBlank(fromHeader) && !isGenericMimeType(fromHeader)) {
            return fromHeader.trim();
        }
        return mimeTypeFromFileName(getFileName(file))
                .orElse(StringUtils.defaultIfBlank(fromHeader, DEFAULT_MIME_TYPE));
    }

    public static Optional<String> mimeTypeFromFileName(String fileNameOrUrl) {
        String ext = extensionFromFileName(fileNameOrUrl);
        if (ext.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(MIME_BY_EXTENSION.get(ext));
    }

    public static String extensionFromFileName(String fileNameOrUrl) {
        if (StringUtils.isBlank(fileNameOrUrl)) {
            return "";
        }
        String name = fileNameOrUrl.trim();
        int query = name.indexOf('?');
        if (query >= 0) {
            name = name.substring(0, query);
        }
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isGenericMimeType(String contentType) {
        return DEFAULT_MIME_TYPE.equalsIgnoreCase(contentType)
                || "application/binary".equalsIgnoreCase(contentType)
                || "binary/octet-stream".equalsIgnoreCase(contentType);
    }
}
