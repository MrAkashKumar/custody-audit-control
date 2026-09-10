package com.custody.app.scheduler;

import com.custody.app.config.properties.RetentionProperties;
import com.custody.core.utils.Ids;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class RetentionJob {
  private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);
  private final RetentionProperties policy;
  private final Clock clock;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public RetentionJob(
      RetentionProperties retentionProperties,
      Clock clock,
      @Value("${spring.datasource.url}") String url) {
    this.policy = retentionProperties;
    this.clock = clock;
    var driverManagerDataSource =
        new DriverManagerDataSource(
            url, retentionProperties.username(), retentionProperties.password());
    this.jdbc = new JdbcTemplate(driverManagerDataSource);
    this.transaction =
        new TransactionTemplate(new DataSourceTransactionManager(driverManagerDataSource));
  }

  @Scheduled(cron = "${cc.retention.cron}", zone = "UTC")
  public void scheduled() {
    if (policy.enabled()) runOnce();
  }

  public long runOnce() {
    try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", Ids.next())) {
      return transaction.execute(
          status -> {
            Instant cutoff =
                clock.instant().atZone(ZoneOffset.UTC).minusYears(policy.years()).toInstant();
            Long eligible =
                jdbc.queryForObject(
                    "select count(*) from cc_audit_event where occurred_at < ?",
                    Long.class,
                    java.sql.Timestamp.from(cutoff));
            int deleted = 0;
            if (!policy.dryRun()) {
              // Select a bounded batch, then recheck expiry on deletion; portable to H2 and MySQL.
              var expiredEventIds =
                  jdbc.queryForList(
                      "select id from cc_audit_event where occurred_at < ? order by occurred_at,id limit ?",
                      String.class,
                      java.sql.Timestamp.from(cutoff),
                      policy.batchSize());
              for (String expiredEventId : expiredEventIds) {
                deleted +=
                    jdbc.update(
                        "delete from cc_audit_event where id = ? and occurred_at < ?",
                        expiredEventId,
                        java.sql.Timestamp.from(cutoff));
              }
            }
            jdbc.update(
                "insert into cc_retention_run(id,started_at,cutoff,eligible_count,deleted_count,dry_run,trace_id) values(?,?,?,?,?,?,?)",
                Ids.next(),
                java.sql.Timestamp.from(clock.instant()),
                java.sql.Timestamp.from(cutoff),
                eligible,
                deleted,
                policy.dryRun(),
                MDC.get("traceId"));
            log.info(
                "event=retention_batch eligible={} deleted={} dryRun={}",
                eligible,
                deleted,
                policy.dryRun());
            return (long) deleted;
          });
    } catch (Exception exception) {
      log.error("event=retention_failure", exception);
      throw exception;
    }
  }
}
