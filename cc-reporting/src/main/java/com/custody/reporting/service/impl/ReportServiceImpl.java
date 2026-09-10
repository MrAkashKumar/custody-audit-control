package com.custody.reporting.service.impl;

import com.custody.audit.dto.request.AuditSearchRequest;
import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.audit.service.AuditService;
import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.Action;
import com.custody.core.enums.AuditCategory;
import com.custody.core.exception.AppException;
import com.custody.core.service.AuditWriter;
import com.custody.core.utils.Ids;
import com.custody.identity.service.AccessPolicy;
import com.custody.identity.service.IdentityService;
import com.custody.reporting.config.properties.ReportProperties;
import com.custody.reporting.dto.ReportDownload;
import com.custody.reporting.dto.request.CreateScheduleRequest;
import com.custody.reporting.dto.request.GenerateReportRequest;
import com.custody.reporting.dto.response.ReportResponse;
import com.custody.reporting.dto.response.ScheduleResponse;
import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.enums.ReportStatus;
import com.custody.reporting.error.ReportingErrorCatalog;
import com.custody.reporting.mapper.ReportResponseMapper;
import com.custody.reporting.model.ReportArtifact;
import com.custody.reporting.model.ReportJob;
import com.custody.reporting.model.ReportSchedule;
import com.custody.reporting.report.ReportRenderer;
import com.custody.reporting.repository.ReportArtifactRepository;
import com.custody.reporting.repository.ReportJobRepository;
import com.custody.reporting.repository.ReportScheduleRepository;
import com.custody.reporting.service.ReportScheduleService;
import com.custody.reporting.service.ReportService;
import com.custody.reporting.service.ReportWorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportServiceImpl
    implements ReportService, ReportWorkerService, ReportScheduleService {
  private final ReportResponseMapper responseMapper;
  private final ReportJobRepository jobs;
  private final ReportArtifactRepository artifacts;
  private final ReportScheduleRepository schedules;
  private final AccessPolicy access;
  private final IdentityService identities;
  private final AuditService audit;
  private final AuditWriter auditWriter;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final ReportProperties settings;
  private final Map<ReportFormat, ReportRenderer> renderers;

  public ReportServiceImpl(
      ReportResponseMapper responseMapper,
      ReportJobRepository reportJobRepository,
      ReportArtifactRepository artifactRepository,
      ReportScheduleRepository reportScheduleRepository,
      AccessPolicy accessPolicy,
      IdentityService identityService,
      AuditService auditService,
      AuditWriter auditWriter,
      ObjectMapper objectMapper,
      Clock clock,
      ReportProperties reportProperties,
      List<ReportRenderer> reportRenderers) {
    this.responseMapper = responseMapper;
    this.jobs = reportJobRepository;
    this.artifacts = artifactRepository;
    this.schedules = reportScheduleRepository;
    this.access = accessPolicy;
    this.identities = identityService;
    this.audit = auditService;
    this.auditWriter = auditWriter;
    this.mapper = objectMapper;
    this.clock = clock;
    this.settings = reportProperties;
    Map<ReportFormat, ReportRenderer> map = new EnumMap<>(ReportFormat.class);
    reportRenderers.forEach(
        candidateReportRenderer -> {
          if (map.put(candidateReportRenderer.format(), candidateReportRenderer) != null)
            throw new IllegalStateException("Duplicate renderer");
        });
    this.renderers = Map.copyOf(map);
  }

  @Override
  public ReportResponse request(String actor, GenerateReportRequest request) {
    return create(actor, request, null, null);
  }

  private ReportResponse create(
      String actor, GenerateReportRequest request, String group, String occurrence) {
    AppException.require(
        !access.grants(actor, Action.GENERATE).isEmpty(), ReportingErrorCatalog.ACTION_FORBIDDEN);
    audit.query(actor, request.filter(), 0, 1, clock.instant(), Action.GENERATE);
    ReportJob reportJob = new ReportJob();
    reportJob.setId(Ids.next());
    reportJob.setOwner(actor);
    reportJob.setAudienceGroup(group);
    reportJob.setTriggerType(occurrence == null ? "MANUAL" : "SCHEDULED");
    reportJob.setOccurrenceKey(occurrence);
    reportJob.setFormat(request.format());
    reportJob.setStatus(ReportStatus.REQUESTED);
    reportJob.setCriteria(encode(request.filter()));
    reportJob.setRequestedAt(clock.instant());
    reportJob.setExpiresAt(reportJob.getRequestedAt().plus(Duration.ofDays(settings.expiryDays())));
    reportJob.setTraceId(Objects.toString(MDC.get("traceId"), Ids.next()));
    jobs.saveAndFlush(reportJob);
    recordReportActivity(reportJob, actor, "REQUESTED");
    return responseMapper.toReportResponse(reportJob);
  }

  @Override
  public List<ReportResponse> list(String actor) {
    return jobs.findAll().stream()
        .filter(reportJob -> canDownloadReport(actor, reportJob))
        .sorted(
            Comparator.comparing((ReportJob reportJob) -> reportJob.getRequestedAt()).reversed())
        .map(responseMapper::toReportResponse)
        .toList();
  }

  private boolean canDownloadReport(String actor, ReportJob reportJob) {
    boolean audience =
        reportJob.getOwner().equals(actor)
            || (reportJob.getAudienceGroup() != null
                && identities.session(actor).groups().stream()
                    .anyMatch(
                        groupResponse -> groupResponse.id().equals(reportJob.getAudienceGroup())));
    if (!audience) return false;
    if (reportJob.getAllowedPairs() == null)
      return !access.grants(actor, Action.DOWNLOAD).isEmpty();
    for (String pair : decodePairs(reportJob.getAllowedPairs())) {
      String[] parts = pair.split("\\|", 2);
      if (!access.allows(actor, parts[0], Action.DOWNLOAD, parts[1])) return false;
      if (reportJob.getFieldFootprint() != null) {
        try {
          var footprint = mapper.readTree(reportJob.getFieldFootprint()).get(pair);
          if (footprint != null)
            for (var field : footprint)
              if (access
                  .hiddenFields(actor, parts[0], Action.DOWNLOAD, parts[1])
                  .contains(field.asText())) return false;
        } catch (java.io.IOException iOException) {
          throw new IllegalStateException("Invalid report footprint", iOException);
        }
      }
    }
    return !access.grants(actor, Action.DOWNLOAD).isEmpty();
  }

  @Override
  public ReportDownload download(String actor, String id) {
    ReportJob reportJob =
        jobs.findById(id)
            .filter(candidateReportJob -> canDownloadReport(actor, candidateReportJob))
            .orElseThrow(() -> AppException.of(ReportingErrorCatalog.RESOURCE_NOT_AVAILABLE));
    AppException.require(
        reportJob.getExpiresAt().isAfter(clock.instant()), ReportingErrorCatalog.REPORT_EXPIRED);
    AppException.require(
        reportJob.getStatus() == ReportStatus.READY, ReportingErrorCatalog.REPORT_NOT_READY);
    recordReportActivity(reportJob, actor, "DOWNLOAD_REQUESTED");
    return new ReportDownload(
        artifacts
            .findById(reportJob.getId())
            .orElseThrow(() -> AppException.of(ReportingErrorCatalog.RESOURCE_NOT_AVAILABLE))
            .getContent(),
        "audit-" + reportJob.getId() + "." + reportJob.getFormat().name().toLowerCase(Locale.ROOT),
        reportJob.getFormat() == ReportFormat.CSV
            ? "text/csv;charset=UTF-8"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  @Override
  public void process(String id) {
    ReportJob reportJob = jobs.lock(id).orElseThrow();
    if (reportJob.getStatus() != ReportStatus.REQUESTED
        && reportJob.getStatus() != ReportStatus.RUNNING) return;
    AuditSearchRequest auditSearchRequest =
        decode(reportJob.getCriteria(), AuditSearchRequest.class);
    List<AuditEventResponse> events = new ArrayList<>();
    Set<String> pairs = new TreeSet<>();
    for (int page = 0; ; page++) {
      var result =
          audit.query(
              reportJob.getOwner(),
              auditSearchRequest,
              page,
              500,
              reportJob.getRequestedAt(),
              Action.GENERATE);
      AppException.require(
          result.totalElements() <= settings.maxRows(), ReportingErrorCatalog.REPORT_TOO_LARGE);
      events.addAll(result.content());
      if (events.size() >= result.totalElements()) break;
    }
    for (var auditEventResponse : events)
      pairs.add(auditEventResponse.feature() + "|" + auditEventResponse.scope());
    // Empty reports still require the requested feature/scope permission on download.
    if (events.isEmpty()
        && auditSearchRequest.feature() != null
        && auditSearchRequest.scope() != null)
      pairs.add(auditSearchRequest.feature() + "|" + auditSearchRequest.scope());
    AppException.require(
        !access.grants(reportJob.getOwner(), Action.GENERATE).isEmpty(),
        ReportingErrorCatalog.ACTION_FORBIDDEN);
    reportJob.setAllowedPairs(encode(pairs));
    Map<String, Set<String>> footprint = new TreeMap<>();
    for (var auditEventResponse : events) {
      var fields =
          footprint.computeIfAbsent(
              auditEventResponse.feature() + "|" + auditEventResponse.scope(),
              featureScopePair -> new TreeSet<>());
      fields.addAll(auditEventResponse.before().keySet());
      fields.addAll(auditEventResponse.proposed().keySet());
      fields.addAll(auditEventResponse.after().keySet());
    }
    reportJob.setFieldFootprint(encode(footprint));
    artifacts.save(
        new ReportArtifact(
            reportJob.getId(),
            renderers.get(reportJob.getFormat()).render(events, reportJob.getCriteria())));
    reportJob.setRowCount(events.size());
    reportJob.setStatus(ReportStatus.READY);
    reportJob.setReadyAt(clock.instant());
    jobs.save(reportJob);
    recordReportActivity(reportJob, reportJob.getOwner(), "GENERATED");
  }

  @Override
  public void failed(String id, String code) {
    jobs.findById(id)
        .filter(
            reportJob ->
                reportJob.getStatus() == ReportStatus.REQUESTED
                    || reportJob.getStatus() == ReportStatus.RUNNING)
        .ifPresent(
            reportJob -> {
              reportJob.setStatus(ReportStatus.FAILED);
              reportJob.setFailureCode(code);
              jobs.save(reportJob);
              recordReportActivity(reportJob, reportJob.getOwner(), "FAILED");
            });
  }

  @Override
  public boolean claim(String id) {
    var job = jobs.lock(id).orElseThrow();
    if (job.getStatus() != ReportStatus.REQUESTED
        && (job.getStatus() != ReportStatus.RUNNING
            || job.getClaimedAt().isAfter(clock.instant().minusSeconds(300)))) return false;
    job.setStatus(ReportStatus.RUNNING);
    job.setClaimedAt(clock.instant());
    jobs.save(job);
    return true;
  }

  @Override
  public List<String> queued() {
    return jobs
        .available(
            clock.instant().minusSeconds(300),
            org.springframework.data.domain.PageRequest.of(0, 10))
        .stream()
        .map(reportJob -> reportJob.getId())
        .toList();
  }

  @Override
  public List<ScheduleResponse> schedules(String actor) {
    return schedules.findAll().stream()
        .filter(reportSchedule -> reportSchedule.getOwner().equals(actor))
        .map(responseMapper::toScheduleResponse)
        .toList();
  }

  @Override
  public ScheduleResponse schedule(String actor, CreateScheduleRequest createScheduleRequest) {
    access.require(
        actor, createScheduleRequest.feature(), Action.SCHEDULE, createScheduleRequest.scope());
    access.require(
        actor, createScheduleRequest.feature(), Action.GENERATE, createScheduleRequest.scope());
    if (createScheduleRequest.audienceGroup() != null
        && !createScheduleRequest.audienceGroup().isBlank())
      AppException.require(
          identities.session(actor).groups().stream()
              .anyMatch(
                  groupResponse ->
                      groupResponse.id().equals(createScheduleRequest.audienceGroup())),
          ReportingErrorCatalog.ACTION_FORBIDDEN);
    ReportSchedule reportSchedule = new ReportSchedule();
    reportSchedule.setId(Ids.next());
    reportSchedule.setName(createScheduleRequest.name());
    reportSchedule.setOwner(actor);
    reportSchedule.setAudienceGroup(
        createScheduleRequest.audienceGroup() == null
                || createScheduleRequest.audienceGroup().isBlank()
            ? null
            : createScheduleRequest.audienceGroup());
    reportSchedule.setCategory(createScheduleRequest.category());
    reportSchedule.setFeature(createScheduleRequest.feature());
    reportSchedule.setScope(createScheduleRequest.scope());
    reportSchedule.setFormat(createScheduleRequest.format());
    reportSchedule.setIntervalDays(createScheduleRequest.intervalDays());
    reportSchedule.setEnabled(true);
    reportSchedule.setNextRunAt(
        LocalDate.now(clock.withZone(ZoneId.of("Asia/Singapore")))
            .plusDays(1)
            .atStartOfDay(ZoneId.of("Asia/Singapore"))
            .toInstant());
    schedules.save(reportSchedule);
    auditWriter.append(
        AuditDraft.builder(AuditCategory.BUSINESS)
            .feature(reportSchedule.getFeature())
            .scope(reportSchedule.getScope())
            .recordId(reportSchedule.getId())
            .reference(reportSchedule.getId())
            .action("SCHEDULE")
            .status("CREATED")
            .actor(actor)
            .maker(actor)
            .checker("")
            .reason("Report schedule created")
            .source("Reports")
            .before(Map.of())
            .after(
                Map.of(
                    "name",
                    reportSchedule.getName(),
                    "intervalDays",
                    Integer.toString(reportSchedule.getIntervalDays())))
            .build());
    return responseMapper.toScheduleResponse(reportSchedule);
  }

  @Override
  public ScheduleResponse toggle(String actor, String id) {
    ReportSchedule reportSchedule =
        schedules
            .lock(id)
            .filter(candidateReportSchedule -> candidateReportSchedule.getOwner().equals(actor))
            .orElseThrow(() -> AppException.of(ReportingErrorCatalog.RESOURCE_NOT_AVAILABLE));
    access.require(actor, reportSchedule.getFeature(), Action.SCHEDULE, reportSchedule.getScope());
    reportSchedule.setEnabled(!reportSchedule.getEnabled());
    schedules.save(reportSchedule);
    auditWriter.append(
        AuditDraft.builder(AuditCategory.BUSINESS)
            .feature(reportSchedule.getFeature())
            .scope(reportSchedule.getScope())
            .recordId(reportSchedule.getId())
            .reference(reportSchedule.getId())
            .action("SCHEDULE")
            .status(reportSchedule.getEnabled() ? "ENABLED" : "DISABLED")
            .actor(actor)
            .maker(actor)
            .checker("")
            .reason("Schedule status changed")
            .source("Reports")
            .before(Map.of("enabled", Boolean.toString(!reportSchedule.getEnabled())))
            .after(Map.of("enabled", Boolean.toString(reportSchedule.getEnabled())))
            .build());
    return responseMapper.toScheduleResponse(reportSchedule);
  }

  @Override
  public List<String> due() {
    return schedules.findAll().stream()
        .filter(
            reportSchedule ->
                reportSchedule.getEnabled()
                    && !reportSchedule.getNextRunAt().isAfter(clock.instant()))
        .map(reportSchedule -> reportSchedule.getId())
        .toList();
  }

  @Override
  public void dispatch(String id) {
    ReportSchedule reportSchedule = schedules.lock(id).orElseThrow();
    if (!reportSchedule.getEnabled() || reportSchedule.getNextRunAt().isAfter(clock.instant()))
      return;
    if (!access.allows(
            reportSchedule.getOwner(),
            reportSchedule.getFeature(),
            Action.SCHEDULE,
            reportSchedule.getScope())
        || !access.allows(
            reportSchedule.getOwner(),
            reportSchedule.getFeature(),
            Action.GENERATE,
            reportSchedule.getScope())) {
      reportSchedule.setEnabled(false);
      reportSchedule.setLastFailure("SCHEDULE_AUTHORITY_REVOKED");
      schedules.save(reportSchedule);
      return;
    }
    LocalDate end =
        reportSchedule
            .getNextRunAt()
            .atZone(ZoneId.of("Asia/Singapore"))
            .toLocalDate()
            .minusDays(1);
    var auditSearchRequest =
        new AuditSearchRequest(
            reportSchedule.getCategory(),
            reportSchedule.getFeature(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            reportSchedule.getScope(),
            end.minusDays(reportSchedule.getIntervalDays() - 1),
            end);
    create(
        reportSchedule.getOwner(),
        new GenerateReportRequest(reportSchedule.getFormat(), auditSearchRequest),
        reportSchedule.getAudienceGroup(),
        reportSchedule.getId() + ":" + reportSchedule.getNextRunAt());
    reportSchedule.setNextRunAt(
        reportSchedule.getNextRunAt().plus(Duration.ofDays(reportSchedule.getIntervalDays())));
    schedules.save(reportSchedule);
  }

  @Override
  public void expire() {
    jobs.findAll().stream()
        .filter(
            reportJob ->
                reportJob.getStatus() != ReportStatus.EXPIRED
                    && !reportJob.getExpiresAt().isAfter(clock.instant()))
        .forEach(
            reportJob -> {
              artifacts.deleteById(reportJob.getId());
              reportJob.setStatus(ReportStatus.EXPIRED);
              jobs.save(reportJob);
            });
  }

  private void recordReportActivity(ReportJob reportJob, String actor, String status) {
    var auditSearchRequest = decode(reportJob.getCriteria(), AuditSearchRequest.class);
    String feature =
        auditSearchRequest.feature() == null || auditSearchRequest.feature().isBlank()
            ? "vault"
            : auditSearchRequest.feature();
    String scope =
        auditSearchRequest.scope() == null || auditSearchRequest.scope().isBlank()
            ? access.scopes(reportJob.getOwner(), feature, Action.GENERATE).stream()
                .sorted()
                .findFirst()
                .orElse("SYSTEM")
            : auditSearchRequest.scope();
    auditWriter.append(
        AuditDraft.builder(AuditCategory.BUSINESS)
            .feature(feature)
            .scope(scope)
            .recordId(reportJob.getId())
            .reference(reportJob.getId())
            .action("EXPORT")
            .status(status)
            .actor(actor)
            .maker(reportJob.getOwner())
            .checker("")
            .reason("Report " + status.toLowerCase(Locale.ROOT))
            .source("Reports")
            .before(Map.of())
            .after(
                Map.of("format", reportJob.getFormat().name(), "criteria", reportJob.getCriteria()))
            .build());
  }

  private String encode(Object valueToSerialize) {
    try {
      return mapper.writeValueAsString(valueToSerialize);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot encode report metadata", exception);
    }
  }

  private <T> T decode(String serializedValue, Class<T> type) {
    try {
      return mapper.readValue(serializedValue, type);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot read report metadata", exception);
    }
  }

  private List<String> decodePairs(String serializedValue) {
    try {
      return mapper.readValue(
          serializedValue, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot read report scope", exception);
    }
  }
}
