package com.custody.reporting.dto;

public record ReportDownload(byte[] bytes, String filename, String contentType) {}
