package com.hamza.checkupdates.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UpdateInfo {
    private String version;
    private LocalDateTime releaseDate;
    private String downloadUrl;
    private Map<String, String> changelog;
    private boolean required;
    private String minSupportedVersion;
    private Long fileSize;
    private String checksum;
}
