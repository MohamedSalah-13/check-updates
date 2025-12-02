package com.hamza.checkupdates.model;

import lombok.Data;

@Data
public class ClientInfo {
    private String clientId;
    private String currentVersion;
    private String osName;
    private String osVersion;
    private String javaVersion;
}