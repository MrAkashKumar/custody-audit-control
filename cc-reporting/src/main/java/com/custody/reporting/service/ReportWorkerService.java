package com.custody.reporting.service;

import java.util.List;

/** Background report execution contract. */
public interface ReportWorkerService {
  void process(String id);

  void failed(String id, String code);

  boolean claim(String id);

  List<String> queued();

  void expire();
}
