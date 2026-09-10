package com.custody.audit.service.impl;

import com.custody.audit.dto.request.AuditSearchRequest;
import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.audit.dto.response.AuditPageResponse;
import com.custody.audit.error.AuditErrorCatalog;
import com.custody.audit.model.AuditEvent;
import com.custody.audit.repository.AuditRepository;
import com.custody.audit.service.AuditService;
import com.custody.core.enums.Action;
import com.custody.core.exception.AppException;
import com.custody.core.utils.JsonValues;
import com.custody.identity.service.AccessPolicy;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {
  private final AuditRepository auditRepository;
  private final JsonValues jsonValues;
  private final Clock clock;
  private final AccessPolicy accessPolicy;

  public AuditServiceImpl(
      AuditRepository auditRepository,
      JsonValues jsonValues,
      Clock clock,
      AccessPolicy accessPolicy) {
    this.auditRepository = auditRepository;
    this.jsonValues = jsonValues;
    this.clock = clock;
    this.accessPolicy = accessPolicy;
  }

  @Transactional(readOnly = true)
  @Override
  public AuditPageResponse search(
      String actor, AuditSearchRequest auditSearchRequest, int page, int size) {
    return query(actor, auditSearchRequest, page, size, clock.instant(), Action.AUDIT);
  }

  @Transactional(readOnly = true)
  @Override
  public AuditPageResponse query(
      String actor,
      AuditSearchRequest auditSearchRequest,
      int page,
      int size,
      Instant cutoff,
      Action permission) {
    AppException.require(page >= 0 && size >= 1 && size <= 500, AuditErrorCatalog.INPUT_INVALID);
    if (auditSearchRequest.from() != null && auditSearchRequest.to() != null)
      AppException.require(
          !auditSearchRequest.from().isAfter(auditSearchRequest.to()),
          AuditErrorCatalog.INPUT_INVALID);
    var result =
        auditRepository.findAll(
            buildAuthorisedSearchSpecification(actor, auditSearchRequest, cutoff, permission),
            PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
    return new AuditPageResponse(
        result.getContent().stream()
            .map(auditEvent -> toAuthorisedAuditResponse(auditEvent, actor, permission))
            .toList(),
        result.getTotalElements(),
        page,
        size);
  }

  @Transactional(readOnly = true)
  @Override
  public AuditEventResponse detail(String actor, String id) {
    AuditEvent auditEvent =
        auditRepository
            .findById(id)
            .filter(
                candidateAuditEvent ->
                    accessPolicy.allows(
                        actor,
                        candidateAuditEvent.getFeature(),
                        Action.AUDIT,
                        candidateAuditEvent.getScope()))
            .orElseThrow(() -> AppException.of(AuditErrorCatalog.RESOURCE_NOT_AVAILABLE));
    return toAuthorisedAuditResponse(auditEvent, actor, Action.AUDIT);
  }

  /** Combines each permitted feature/scope pair with user filters before pagination. */
  private Specification<AuditEvent> buildAuthorisedSearchSpecification(
      String actor, AuditSearchRequest auditSearchRequest, Instant cutoff, Action permission) {
    // Grants retain feature/scope pairing. Enumerating evidence feature keys never exposes them to
    // the client.
    return (auditEventRoot, criteriaQuery, criteriaBuilder) -> {
      List<Predicate> permissionPredicates = new ArrayList<>();
      for (var grant : accessPolicy.grants(actor, permission)) {
        Predicate feature =
            grant.feature().equals("*")
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(auditEventRoot.get("feature"), grant.feature());
        permissionPredicates.add(
            criteriaBuilder.and(
                feature, criteriaBuilder.equal(auditEventRoot.get("scope"), grant.scope())));
      }
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(
          permissionPredicates.isEmpty()
              ? criteriaBuilder.disjunction()
              : criteriaBuilder.or(permissionPredicates.toArray(Predicate[]::new)));
      predicates.add(criteriaBuilder.lessThanOrEqualTo(auditEventRoot.get("occurredAt"), cutoff));
      addExactMatchPredicate(
          predicates, criteriaBuilder, auditEventRoot, "category", auditSearchRequest.category());
      addExactMatchPredicate(
          predicates, criteriaBuilder, auditEventRoot, "feature", auditSearchRequest.feature());
      addExactMatchPredicate(
          predicates, criteriaBuilder, auditEventRoot, "scope", auditSearchRequest.scope());
      addExactMatchPredicate(
          predicates, criteriaBuilder, auditEventRoot, "action", auditSearchRequest.action());
      addExactMatchPredicate(
          predicates, criteriaBuilder, auditEventRoot, "status", auditSearchRequest.status());
      addContainsPredicate(
          predicates, criteriaBuilder, auditEventRoot, "recordId", auditSearchRequest.record());
      addContainsPredicate(
          predicates,
          criteriaBuilder,
          auditEventRoot,
          "referenceId",
          auditSearchRequest.reference());
      if (auditSearchRequest.screen() != null && !auditSearchRequest.screen().isBlank()) {
        String sourcePattern = escapedContainsPattern(auditSearchRequest.screen());
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("source")), sourcePattern, '\\'),
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("endpoint")), sourcePattern, '\\')));
      }
      if (auditSearchRequest.user() != null && !auditSearchRequest.user().isBlank()) {
        String userPattern = escapedContainsPattern(auditSearchRequest.user());
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("actor")), userPattern, '\\'),
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("maker")), userPattern, '\\'),
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("checker")), userPattern, '\\')));
      }
      if (auditSearchRequest.field() != null && !auditSearchRequest.field().isBlank()) {
        String fieldPattern = escapedContainsPattern("\"" + auditSearchRequest.field() + "\"");
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("beforeValues")), fieldPattern, '\\'),
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("proposedValues")),
                    fieldPattern,
                    '\\'),
                criteriaBuilder.like(
                    criteriaBuilder.lower(auditEventRoot.get("afterValues")), fieldPattern, '\\')));
      }
      ZoneId reportingZone = ZoneId.of("Asia/Singapore");
      if (auditSearchRequest.from() != null)
        predicates.add(
            criteriaBuilder.greaterThanOrEqualTo(
                auditEventRoot.get("occurredAt"),
                auditSearchRequest.from().atStartOfDay(reportingZone).toInstant()));
      if (auditSearchRequest.to() != null)
        predicates.add(
            criteriaBuilder.lessThan(
                auditEventRoot.get("occurredAt"),
                auditSearchRequest.to().plusDays(1).atStartOfDay(reportingZone).toInstant()));
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String escapedContainsPattern(String filterValue) {
    return "%"
        + filterValue
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        + "%";
  }

  private static void addExactMatchPredicate(
      List<Predicate> predicates,
      jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
      jakarta.persistence.criteria.Root<AuditEvent> auditEventRoot,
      String fieldName,
      String filterValue) {
    if (filterValue != null && !filterValue.isBlank())
      predicates.add(criteriaBuilder.equal(auditEventRoot.get(fieldName), filterValue));
  }

  private static void addContainsPredicate(
      List<Predicate> predicates,
      jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
      jakarta.persistence.criteria.Root<AuditEvent> auditEventRoot,
      String fieldName,
      String filterValue) {
    if (filterValue != null && !filterValue.isBlank())
      predicates.add(
          criteriaBuilder.like(
              criteriaBuilder.lower(auditEventRoot.get(fieldName)),
              escapedContainsPattern(filterValue),
              '\\'));
  }

  /**
   * Removes fields hidden by current grants; export also respects audit and download restrictions.
   */
  private AuditEventResponse toAuthorisedAuditResponse(
      AuditEvent auditEvent, String actor, Action permission) {
    Set<String> hiddenFields =
        new HashSet<>(
            accessPolicy.hiddenFields(
                actor, auditEvent.getFeature(), permission, auditEvent.getScope()));
    if (permission == Action.GENERATE) {
      hiddenFields.addAll(
          accessPolicy.hiddenFields(
              actor, auditEvent.getFeature(), Action.AUDIT, auditEvent.getScope()));
      hiddenFields.addAll(
          accessPolicy.hiddenFields(
              actor, auditEvent.getFeature(), Action.DOWNLOAD, auditEvent.getScope()));
    }
    Map<String, String> visibleBeforeValues =
        new TreeMap<>(jsonValues.read(auditEvent.getBeforeValues()));
    Map<String, String> visibleProposedValues =
        new TreeMap<>(jsonValues.read(auditEvent.getProposedValues()));
    Map<String, String> visibleAfterValues =
        new TreeMap<>(jsonValues.read(auditEvent.getAfterValues()));
    hiddenFields.forEach(
        fieldName -> {
          visibleBeforeValues.remove(fieldName);
          visibleProposedValues.remove(fieldName);
          visibleAfterValues.remove(fieldName);
        });
    return new AuditEventResponse(
        auditEvent.getId(),
        auditEvent.getSchemaVersion(),
        auditEvent.getEventType(),
        auditEvent.getCategory(),
        auditEvent.getFeature(),
        auditEvent.getScope(),
        auditEvent.getRecordId(),
        auditEvent.getReferenceId(),
        auditEvent.getAction(),
        auditEvent.getStatus(),
        auditEvent.getActor(),
        auditEvent.getMaker(),
        auditEvent.getChecker(),
        auditEvent.getReason(),
        auditEvent.getSource(),
        auditEvent.getTraceId(),
        auditEvent.getEndpoint(),
        auditEvent.getHttpMethod(),
        auditEvent.getHttpStatus(),
        auditEvent.getOutcome(),
        auditEvent.getErrorCode(),
        auditEvent.getSourceIp(),
        auditEvent.getUserAgent(),
        auditEvent.getRiskLevel(),
        auditEvent.getDurationMs(),
        jsonValues.read(auditEvent.getMetadata()),
        auditEvent.getOccurredAt(),
        visibleBeforeValues,
        visibleProposedValues,
        visibleAfterValues);
  }
}
