package com.custody.reporting.service;

import com.custody.reporting.dto.ReportDownload;
import com.custody.reporting.dto.request.GenerateReportRequest;
import com.custody.reporting.dto.response.ReportResponse;
import java.util.List;

/** User-facing report access. */
public interface ReportService {
  ReportResponse request(String actor, GenerateReportRequest request);

  List<ReportResponse> list(String actor);

  ReportDownload download(String actor, String id);
}
