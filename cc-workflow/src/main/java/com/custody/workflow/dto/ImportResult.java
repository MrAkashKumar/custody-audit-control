package com.custody.workflow.dto;

import java.util.List;

public record ImportResult(String batchId, int submitted, List<String> changeIds) {}
