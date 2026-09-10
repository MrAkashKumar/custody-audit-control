package com.custody.reporting.mapper;

import com.custody.reporting.dto.response.ReportResponse;
import com.custody.reporting.dto.response.ScheduleResponse;
import com.custody.reporting.enums.ReportStatus;
import com.custody.reporting.model.ReportJob;
import com.custody.reporting.model.ReportSchedule;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class ReportResponseMapper {
  private final Clock clock;

  public ReportResponseMapper(Clock clock) {
    this.clock = clock;
  }

  public ReportResponse toReportResponse(ReportJob reportJob) {
    return new ReportResponse(
        reportJob.getId(),
        reportJob.getOwner(),
        reportJob.getAudienceGroup(),
        reportJob.getTriggerType(),
        reportJob.getFormat(),
        !reportJob.getExpiresAt().isAfter(clock.instant())
            ? ReportStatus.EXPIRED
            : reportJob.getStatus(),
        reportJob.getRowCount(),
        reportJob.getRequestedAt(),
        reportJob.getExpiresAt(),
        reportJob.getFailureCode(),
        reportJob.getTraceId());
  }

  public ScheduleResponse toScheduleResponse(ReportSchedule reportSchedule) {
    return new ScheduleResponse(
        reportSchedule.getId(),
        reportSchedule.getName(),
        reportSchedule.getOwner(),
        reportSchedule.getAudienceGroup(),
        reportSchedule.getCategory(),
        reportSchedule.getFeature(),
        reportSchedule.getScope(),
        reportSchedule.getFormat(),
        reportSchedule.getEnabled(),
        reportSchedule.getIntervalDays(),
        reportSchedule.getNextRunAt(),
        reportSchedule.getLastFailure());
  }
}
