package com.custody.reporting.service;

import com.custody.reporting.dto.request.CreateScheduleRequest;
import com.custody.reporting.dto.response.ScheduleResponse;
import java.util.List;

/** Dedicated scheduling contract. */
public interface ReportScheduleService {
  List<ScheduleResponse> schedules(String actor);

  ScheduleResponse schedule(String actor, CreateScheduleRequest createScheduleRequest);

  ScheduleResponse toggle(String actor, String id);

  List<String> due();

  void dispatch(String id);
}
