package com.custody.app.scheduler;

import com.custody.app.error.SystemErrorCatalog;
import com.custody.core.exception.AppException;
import com.custody.core.utils.Ids;
import com.custody.reporting.service.ReportScheduleService;
import com.custody.reporting.service.ReportWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class JobRunner {
  private static final Logger log = LoggerFactory.getLogger(JobRunner.class);
  private final ReportWorkerService reports;
  private final ReportScheduleService reportSchedules;

  JobRunner(ReportWorkerService reports, ReportScheduleService reportSchedules) {
    this.reports = reports;
    this.reportSchedules = reportSchedules;
  }

  @Scheduled(fixedDelayString = "${cc.jobs.poll-ms:5000}")
  public void tick() {
    try (@SuppressWarnings("PMD.UnusedLocalVariable")
        MDC.MDCCloseable trace = MDC.putCloseable("traceId", Ids.next())) {
      for (String id : reportSchedules.due())
        try {
          reportSchedules.dispatch(id);
        } catch (Exception exception) {
          log.error("event=schedule_failure scheduleId={}", id, exception);
        }
      for (String id : reports.queued())
        try (@SuppressWarnings("PMD.UnusedLocalVariable")
            MDC.MDCCloseable job = MDC.putCloseable("reportId", id)) {
          try {
            if (reports.claim(id)) reports.process(id);
          } catch (Exception exception) {
            String code =
                exception instanceof AppException appException
                    ? appException.code()
                    : SystemErrorCatalog.INTERNAL_ERROR.code();
            log.error("event=report_failure code={}", code, exception);
            reports.failed(id, code);
          }
        }
      reports.expire();
    } catch (Exception exception) {
      log.error("event=worker_failure", exception);
    }
  }
}
