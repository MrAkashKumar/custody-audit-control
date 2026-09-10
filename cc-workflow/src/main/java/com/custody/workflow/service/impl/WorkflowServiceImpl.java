package com.custody.workflow.service.impl;

import com.custody.core.enums.Action;
import com.custody.core.enums.ChangeAction;
import com.custody.core.enums.OperationalStatus;
import com.custody.core.enums.RequestStatus;
import com.custody.core.exception.AppException;
import com.custody.core.service.AuditCapture;
import com.custody.core.utils.Ids;
import com.custody.core.utils.JsonValues;
import com.custody.identity.service.AccessPolicy;
import com.custody.workflow.audit.event.SetupAuditEvent;
import com.custody.workflow.dto.request.ApprovalDecisionRequest;
import com.custody.workflow.dto.request.ChangeSetupStatusRequest;
import com.custody.workflow.dto.request.CreateSetupRequest;
import com.custody.workflow.dto.request.UpdateSetupRequest;
import com.custody.workflow.dto.response.ChangeRequestResponse;
import com.custody.workflow.dto.response.SetupRecordResponse;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.custody.workflow.mapper.WorkflowResponseMapper;
import com.custody.workflow.model.ChangeRequest;
import com.custody.workflow.model.SetupRecord;
import com.custody.workflow.repository.ChangeRequestRepository;
import com.custody.workflow.repository.SetupRecordRepository;
import com.custody.workflow.service.FeatureRegistry;
import com.custody.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {
  private final WorkflowResponseMapper responseMapper;
  private final SetupRecordRepository records;
  private final ChangeRequestRepository requests;
  private final FeatureRegistry features;
  private final AccessPolicy access;
  private final AuditCapture auditCapture;
  private final JsonValues json;
  private final Clock clock;

  public WorkflowServiceImpl(
      WorkflowResponseMapper responseMapper,
      SetupRecordRepository setupRecordRepository,
      ChangeRequestRepository changeRequestRepository,
      FeatureRegistry featureRegistry,
      AccessPolicy accessPolicy,
      AuditCapture auditCapture,
      JsonValues jsonValues,
      Clock clock) {
    this.responseMapper = responseMapper;
    this.records = setupRecordRepository;
    this.requests = changeRequestRepository;
    this.features = featureRegistry;
    this.access = accessPolicy;
    this.auditCapture = auditCapture;
    this.json = jsonValues;
    this.clock = clock;
  }

  @Override
  public List<SetupRecordResponse> records(String actor, String feature) {
    features.get(feature);
    Set<String> scopes = access.scopes(actor, feature, Action.VIEW);
    return records
        .findAll(
            (setupRecordRoot, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.and(
                    criteriaBuilder.equal(setupRecordRoot.get("feature"), feature),
                    setupRecordRoot.get("scope").in(scopes)),
            PageRequest.of(0, 500, Sort.by("businessKey")))
        .stream()
        .map(responseMapper::toRecordResponse)
        .toList();
  }

  @Override
  public List<ChangeRequestResponse> pending(String actor) {
    return requests
        .findAll(
            (changeRequestRoot, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(changeRequestRoot.get("status"), RequestStatus.PENDING))
        .stream()
        .filter(
            changeRequest ->
                access.allows(
                    actor, changeRequest.getFeature(), Action.VIEW, changeRequest.getScope()))
        .sorted(
            Comparator.comparing((ChangeRequest changeRequest) -> changeRequest.getSubmittedAt())
                .reversed())
        .map(responseMapper::toChangeResponse)
        .toList();
  }

  @Override
  public ChangeRequestResponse add(
      String actor, String feature, CreateSetupRequest input, String source) {
    var definition = features.get(feature);
    access.require(actor, feature, Action.ADD, input.scope());
    AppException.require(
        records.count(
                (setupRecordRoot, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.and(
                        criteriaBuilder.equal(setupRecordRoot.get("feature"), feature),
                        criteriaBuilder.equal(setupRecordRoot.get("scope"), input.scope()),
                        criteriaBuilder.equal(
                            setupRecordRoot.get("businessKey"), input.businessKey())))
            == 0,
        WorkflowErrorCatalog.DUPLICATE_KEY);
    return propose(
        actor,
        feature,
        input.scope(),
        input.businessKey(),
        null,
        ChangeAction.ADD,
        0,
        Map.of(),
        definition.validate(input.values()),
        input.reason(),
        source);
  }

  @Override
  public ChangeRequestResponse edit(String actor, String id, UpdateSetupRequest input) {
    SetupRecord setupRecord = record(actor, id, Action.EDIT);
    AppException.require(
        setupRecord.getVersion() == input.version(), WorkflowErrorCatalog.STALE_VERSION);
    Map<String, String> values = features.get(setupRecord.getFeature()).validate(input.values());
    AppException.require(
        !json.read(setupRecord.getData()).equals(values),
        WorkflowErrorCatalog.BUSINESS_RULE_VIOLATION);
    return propose(
        actor,
        setupRecord.getFeature(),
        setupRecord.getScope(),
        setupRecord.getBusinessKey(),
        setupRecord.getId(),
        ChangeAction.EDIT,
        setupRecord.getVersion(),
        snapshot(setupRecord),
        values,
        input.reason(),
        "Setup screen");
  }

  @Override
  public ChangeRequestResponse status(String actor, String id, ChangeSetupStatusRequest input) {
    SetupRecord setupRecord = record(actor, id, Action.STATUS);
    AppException.require(
        setupRecord.getVersion() == input.version(), WorkflowErrorCatalog.STALE_VERSION);
    AppException.require(
        setupRecord.getStatus() != input.status(), WorkflowErrorCatalog.INVALID_TRANSITION);
    return propose(
        actor,
        setupRecord.getFeature(),
        setupRecord.getScope(),
        setupRecord.getBusinessKey(),
        setupRecord.getId(),
        input.status() == OperationalStatus.ACTIVE
            ? ChangeAction.ACTIVATE
            : ChangeAction.DEACTIVATE,
        setupRecord.getVersion(),
        snapshot(setupRecord),
        json.read(setupRecord.getData()),
        input.reason(),
        "Setup screen");
  }

  private ChangeRequestResponse propose(
      String actor,
      String feature,
      String scope,
      String key,
      String record,
      ChangeAction action,
      long version,
      Map<String, String> before,
      Map<String, String> values,
      String reason,
      String source) {
    String pendingKey = feature + ":" + scope + ":" + key;
    AppException.require(
        requests.count(
                (changeRequestRoot, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(changeRequestRoot.get("pendingKey"), pendingKey))
            == 0,
        WorkflowErrorCatalog.REQUEST_ALREADY_PENDING);
    ChangeRequest changeRequest = new ChangeRequest();
    changeRequest.setId(Ids.next());
    changeRequest.setFeature(feature);
    changeRequest.setScope(scope);
    changeRequest.setBusinessKey(key);
    changeRequest.setRecordId(record);
    changeRequest.setAction(action);
    changeRequest.setStatus(RequestStatus.PENDING);
    changeRequest.setBaselineVersion(version);
    changeRequest.setBeforeData(json.write(before));
    changeRequest.setProposedData(json.write(values));
    changeRequest.setMaker(actor);
    changeRequest.setReason(reason);
    changeRequest.setSubmittedAt(clock.instant());
    changeRequest.setPendingKey(pendingKey);
    requests.saveAndFlush(changeRequest);
    ChangeRequestResponse changeResponse = responseMapper.toChangeResponse(changeRequest);
    auditCapture.record(new SetupAuditEvent(changeResponse, actor, source, values));
    return changeResponse;
  }

  @Override
  public ChangeRequestResponse decide(String actor, String id, ApprovalDecisionRequest input) {
    ChangeRequest changeRequest =
        requests
            .findById(id)
            .filter(
                candidateChangeRequest ->
                    access.allows(
                        actor,
                        candidateChangeRequest.getFeature(),
                        Action.APPROVE,
                        candidateChangeRequest.getScope()))
            .orElseThrow(() -> AppException.of(WorkflowErrorCatalog.RESOURCE_NOT_AVAILABLE));
    AppException.require(
        !changeRequest.getMaker().equals(actor), WorkflowErrorCatalog.SELF_APPROVAL_FORBIDDEN);
    AppException.require(
        changeRequest.getStatus() == RequestStatus.PENDING,
        WorkflowErrorCatalog.INVALID_TRANSITION);
    Map<String, String> after = json.read(changeRequest.getProposedData());
    if (input.approve()) {
      after = features.get(changeRequest.getFeature()).validate(after);
      SetupRecord setupRecord;
      if (changeRequest.getAction() == ChangeAction.ADD) {
        setupRecord = new SetupRecord();
        setupRecord.setId(Ids.next());
        setupRecord.setFeature(changeRequest.getFeature());
        setupRecord.setScope(changeRequest.getScope());
        setupRecord.setBusinessKey(changeRequest.getBusinessKey());
        setupRecord.setStatus(OperationalStatus.ACTIVE);
      } else {
        setupRecord =
            records
                .findById(changeRequest.getRecordId())
                .orElseThrow(() -> AppException.of(WorkflowErrorCatalog.RESOURCE_NOT_AVAILABLE));
        AppException.require(
            setupRecord.getVersion() == changeRequest.getBaselineVersion(),
            WorkflowErrorCatalog.STALE_VERSION);
      }
      if (changeRequest.getAction() == ChangeAction.ACTIVATE)
        setupRecord.setStatus(OperationalStatus.ACTIVE);
      if (changeRequest.getAction() == ChangeAction.DEACTIVATE)
        setupRecord.setStatus(OperationalStatus.INACTIVE);
      setupRecord.setData(json.write(after));
      setupRecord.setUpdatedAt(clock.instant());
      records.saveAndFlush(setupRecord);
      changeRequest.setRecordId(setupRecord.getId());
      after = snapshot(setupRecord);
    }
    changeRequest.setStatus(input.approve() ? RequestStatus.APPROVED : RequestStatus.REJECTED);
    changeRequest.setChecker(actor);
    changeRequest.setDecisionReason(input.reason());
    changeRequest.setDecidedAt(clock.instant());
    changeRequest.setPendingKey(null);
    requests.saveAndFlush(changeRequest);
    ChangeRequestResponse changeResponse = responseMapper.toChangeResponse(changeRequest);
    auditCapture.record(new SetupAuditEvent(changeResponse, actor, "Approval review", after));
    return changeResponse;
  }

  private SetupRecord record(String actor, String id, Action action) {
    return records
        .findById(id)
        .filter(
            setupRecord ->
                access.allows(actor, setupRecord.getFeature(), action, setupRecord.getScope()))
        .orElseThrow(() -> AppException.of(WorkflowErrorCatalog.RESOURCE_NOT_AVAILABLE));
  }

  private Map<String, String> snapshot(SetupRecord setupRecord) {
    Map<String, String> values = new TreeMap<>(json.read(setupRecord.getData()));
    values.put("operationalStatus", setupRecord.getStatus().name());
    return values;
  }
}
