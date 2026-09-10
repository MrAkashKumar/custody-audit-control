package com.custody.identity.service.impl;

import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.Action;
import com.custody.core.enums.AuditCategory;
import com.custody.core.exception.AppException;
import com.custody.core.service.AuditWriter;
import com.custody.core.utils.Ids;
import com.custody.core.utils.JsonValues;
import com.custody.identity.dto.AuthenticationCredential;
import com.custody.identity.dto.request.SecurityChangeRequest;
import com.custody.identity.dto.response.DirectoryResponse;
import com.custody.identity.dto.response.GrantResponse;
import com.custody.identity.dto.response.GroupResponse;
import com.custody.identity.dto.response.SecurityChangeResponse;
import com.custody.identity.dto.response.SessionResponse;
import com.custody.identity.dto.response.UserResponse;
import com.custody.identity.error.IdentityErrorCatalog;
import com.custody.identity.mapper.IdentityResponseMapper;
import com.custody.identity.model.Membership;
import com.custody.identity.model.PermissionGrant;
import com.custody.identity.model.SecurityChange;
import com.custody.identity.model.UserAccount;
import com.custody.identity.model.UserGroup;
import com.custody.identity.repository.MembershipRepository;
import com.custody.identity.repository.PermissionGrantRepository;
import com.custody.identity.repository.SecurityChangeRepository;
import com.custody.identity.repository.UserAccountRepository;
import com.custody.identity.repository.UserGroupRepository;
import com.custody.identity.service.AccessPolicy;
import com.custody.identity.service.IdentityService;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IdentityServiceImpl implements IdentityService {
  private final IdentityResponseMapper responseMapper;
  private final UserAccountRepository users;
  private final UserGroupRepository groups;
  private final MembershipRepository members;
  private final PermissionGrantRepository grants;
  private final SecurityChangeRepository changes;
  private final AccessPolicy access;
  private final JsonValues json;
  private final AuditWriter audit;
  private final Clock clock;

  public IdentityServiceImpl(
      IdentityResponseMapper responseMapper,
      UserAccountRepository userAccountRepository,
      UserGroupRepository userGroupRepository,
      MembershipRepository membershipRepository,
      PermissionGrantRepository permissionGrantRepository,
      SecurityChangeRepository securityChangeRepository,
      AccessPolicy accessPolicy,
      JsonValues jsonValues,
      AuditWriter auditWriter,
      Clock clock) {
    this.responseMapper = responseMapper;
    this.users = userAccountRepository;
    this.groups = userGroupRepository;
    this.members = membershipRepository;
    this.grants = permissionGrantRepository;
    this.changes = securityChangeRepository;
    this.access = accessPolicy;
    this.json = jsonValues;
    this.audit = auditWriter;
    this.clock = clock;
  }

  @Override
  public Optional<AuthenticationCredential> credential(String name) {
    return users
        .findById(name)
        .map(
            userAccount ->
                new AuthenticationCredential(
                    userAccount.getId(), userAccount.getPasswordHash(), userAccount.getActive()));
  }

  @Override
  public SessionResponse session(String actor) {
    UserAccount userAccount =
        users
            .findById(actor)
            .filter(candidateUserAccount -> candidateUserAccount.getActive())
            .orElseThrow(() -> AppException.of(IdentityErrorCatalog.AUTHENTICATION_REQUIRED));
    Set<String> ids = new HashSet<>();
    members.findAll().stream()
        .filter(membership -> membership.getUserId().equals(actor))
        .forEach(membership -> ids.add(membership.getGroupId()));
    List<GroupResponse> joined =
        groups.findAll().stream()
            .filter(userGroup -> userGroup.getActive() && ids.contains(userGroup.getId()))
            .map(
                userGroup ->
                    new GroupResponse(
                        userGroup.getId(), userGroup.getName(), userGroup.getActive()))
            .toList();
    Set<String> activeIds = new HashSet<>();
    joined.forEach(groupResponse -> activeIds.add(groupResponse.id()));
    return new SessionResponse(
        userAccount.getId(),
        userAccount.getDisplayName(),
        joined,
        grants.findAll().stream()
            .filter(permissionGrant -> activeIds.contains(permissionGrant.getGroupId()))
            .map(
                permissionGrant ->
                    new GrantResponse(
                        permissionGrant.getId(),
                        permissionGrant.getGroupId(),
                        permissionGrant.getFeature(),
                        permissionGrant.getAction(),
                        permissionGrant.getScope()))
            .toList());
  }

  @Override
  public DirectoryResponse directory(String actor) {
    access.administration(actor);
    return new DirectoryResponse(
        users.findAll().stream()
            .map(
                userAccount ->
                    new UserResponse(
                        userAccount.getId(),
                        userAccount.getDisplayName(),
                        userAccount.getActive(),
                        members.findAll().stream()
                            .filter(
                                membership -> membership.getUserId().equals(userAccount.getId()))
                            .map(membership -> membership.getGroupId())
                            .toList()))
            .toList(),
        groups.findAll().stream()
            .map(
                userGroup ->
                    new GroupResponse(
                        userGroup.getId(), userGroup.getName(), userGroup.getActive()))
            .toList(),
        grants.findAll().stream()
            .map(
                permissionGrant ->
                    new GrantResponse(
                        permissionGrant.getId(),
                        permissionGrant.getGroupId(),
                        permissionGrant.getFeature(),
                        permissionGrant.getAction(),
                        permissionGrant.getScope()))
            .toList(),
        changes.findAll().stream()
            .sorted(
                Comparator.comparing(
                        (SecurityChange securityChange) -> securityChange.getSubmittedAt())
                    .reversed())
            .map(responseMapper::toSecurityChangeResponse)
            .toList());
  }

  @Override
  public SecurityChangeResponse propose(String actor, SecurityChangeRequest input) {
    access.administration(actor);
    validate(input.kind(), input.values());
    SecurityChange securityChange = new SecurityChange();
    securityChange.setId(Ids.next());
    securityChange.setKind(input.kind());
    securityChange.setPayload(json.write(input.values()));
    securityChange.setBaseline(json.write(securitySnapshot(input.kind(), input.values())));
    securityChange.setMaker(actor);
    securityChange.setReason(input.reason());
    securityChange.setStatus("PENDING");
    securityChange.setSubmittedAt(clock.instant());
    changes.save(securityChange);
    audit.append(
        AuditDraft.builder(AuditCategory.ADMIN)
            .feature("administration")
            .scope("SYSTEM")
            .recordId(securityChange.getId())
            .reference(securityChange.getId())
            .action(securityChange.getKind())
            .status("PENDING")
            .actor(actor)
            .maker(actor)
            .checker("")
            .reason(securityChange.getReason())
            .source("Administration")
            .before(Map.of())
            .after(input.values())
            .build());
    return responseMapper.toSecurityChangeResponse(securityChange);
  }

  @Override
  public SecurityChangeResponse decide(String actor, String id, boolean approve, String reason) {
    access.administration(actor);
    SecurityChange securityChange =
        changes
            .findById(id)
            .orElseThrow(() -> AppException.of(IdentityErrorCatalog.RESOURCE_NOT_AVAILABLE));
    AppException.require(
        !securityChange.getMaker().equals(actor), IdentityErrorCatalog.SELF_APPROVAL_FORBIDDEN);
    AppException.require(
        securityChange.getStatus().equals("PENDING"), IdentityErrorCatalog.INVALID_TRANSITION);
    Map<String, String> values = json.read(securityChange.getPayload());
    Map<String, String> before = new LinkedHashMap<>();
    if (approve) {
      validate(securityChange.getKind(), values);
      AppException.require(
          json.read(securityChange.getBaseline())
              .equals(securitySnapshot(securityChange.getKind(), values)),
          IdentityErrorCatalog.STALE_VERSION);
      String group = values.get("groupId");
      switch (securityChange.getKind()) {
        case "MEMBERSHIP" -> {
          String user = values.get("userId");
          before.put(
              "groups",
              String.join(
                  ",",
                  members.findAll().stream()
                      .filter(membership -> membership.getUserId().equals(user))
                      .map(membership -> membership.getGroupId())
                      .sorted()
                      .toList()));
          if (values.get("operation").equals("ADD")) {
            AppException.require(
                members.findAll().stream()
                    .noneMatch(
                        membership ->
                            membership.getUserId().equals(user)
                                && membership.getGroupId().equals(group)),
                IdentityErrorCatalog.DUPLICATE_KEY);
            Membership membership = new Membership();
            membership.setId(Ids.next());
            membership.setUserId(user);
            membership.setGroupId(group);
            members.save(membership);
          } else {
            var existing =
                members.findAll().stream()
                    .filter(
                        membership ->
                            membership.getUserId().equals(user)
                                && membership.getGroupId().equals(group))
                    .toList();
            AppException.require(!existing.isEmpty(), IdentityErrorCatalog.INVALID_TRANSITION);
            members.deleteAll(existing);
          }
        }
        case "GROUP" -> {
          String idValue = values.get("groupId");
          UserGroup userGroup = groups.findById(idValue).orElseGet(UserGroup::new);
          if (userGroup.getId() != null) {
            before.put("name", userGroup.getName());
            before.put("active", Boolean.toString(userGroup.getActive()));
          }
          userGroup.setId(idValue);
          userGroup.setName(values.get("name"));
          userGroup.setActive(Boolean.parseBoolean(values.get("active")));
          groups.save(userGroup);
        }
        case "GRANT" -> {
          if (values.get("operation").equals("REMOVE")) {
            PermissionGrant permissionGrant =
                grants
                    .findById(values.get("grantId"))
                    .orElseThrow(
                        () -> AppException.of(IdentityErrorCatalog.RESOURCE_NOT_AVAILABLE));
            before.putAll(
                Map.of(
                    "groupId",
                    permissionGrant.getGroupId(),
                    "feature",
                    permissionGrant.getFeature(),
                    "action",
                    permissionGrant.getAction(),
                    "scope",
                    permissionGrant.getScope()));
            grants.delete(permissionGrant);
          } else {
            PermissionGrant permissionGrant = new PermissionGrant();
            permissionGrant.setId(Ids.next());
            permissionGrant.setGroupId(group);
            permissionGrant.setFeature(values.get("feature"));
            permissionGrant.setAction(values.get("action"));
            permissionGrant.setScope(values.get("scope"));
            permissionGrant.setHiddenFields(values.getOrDefault("hiddenFields", ""));
            grants.save(permissionGrant);
          }
        }
        default -> throw AppException.of(IdentityErrorCatalog.INPUT_INVALID);
      }
    }
    securityChange.setStatus(approve ? "APPROVED" : "REJECTED");
    securityChange.setChecker(actor);
    securityChange.setDecisionReason(reason);
    securityChange.setDecidedAt(clock.instant());
    changes.saveAndFlush(securityChange);
    audit.append(
        AuditDraft.builder(AuditCategory.ADMIN)
            .feature("administration")
            .scope("SYSTEM")
            .recordId(securityChange.getId())
            .reference(securityChange.getId())
            .action(securityChange.getKind())
            .status(securityChange.getStatus())
            .actor(actor)
            .maker(securityChange.getMaker())
            .checker(actor)
            .reason(reason)
            .source("Administration")
            .before(before)
            .after(values)
            .build());
    return responseMapper.toSecurityChangeResponse(securityChange);
  }

  private Map<String, String> securitySnapshot(String kind, Map<String, String> values) {
    Map<String, String> result = new TreeMap<>();
    if (kind.equals("MEMBERSHIP")) {
      result.put(
          "groups",
          String.join(
              ",",
              members.findAll().stream()
                  .filter(membership -> membership.getUserId().equals(values.get("userId")))
                  .map(membership -> membership.getGroupId())
                  .sorted()
                  .toList()));
    }
    if (kind.equals("GROUP")) {
      groups
          .findById(values.get("groupId"))
          .ifPresent(
              userGroup -> {
                result.put("name", userGroup.getName());
                result.put("active", Boolean.toString(userGroup.getActive()));
                result.put("version", Long.toString(userGroup.getVersion()));
              });
    }
    if (kind.equals("GRANT")) {
      if (values.get("operation").equals("REMOVE")) {
        grants
            .findById(values.get("grantId"))
            .ifPresent(
                permissionGrant -> {
                  result.put("id", permissionGrant.getId());
                  result.put("version", Long.toString(permissionGrant.getVersion()));
                });
      } else
        result.put(
            "grantIds",
            String.join(
                ",",
                grants.findAll().stream()
                    .filter(
                        permissionGrant ->
                            permissionGrant.getGroupId().equals(values.get("groupId"))
                                && permissionGrant.getFeature().equals(values.get("feature"))
                                && permissionGrant.getAction().equals(values.get("action"))
                                && permissionGrant.getScope().equals(values.get("scope")))
                    .map(permissionGrant -> permissionGrant.getId())
                    .sorted()
                    .toList()));
    }
    return result;
  }

  private void validate(String kind, Map<String, String> requestValues) {
    AppException.require(
        Set.of("MEMBERSHIP", "GROUP", "GRANT").contains(kind), IdentityErrorCatalog.INPUT_INVALID);
    AppException.require(
        requestValues.size() <= 6
            && requestValues.values().stream()
                .allMatch(requestValue -> requestValue != null && requestValue.length() <= 120),
        IdentityErrorCatalog.INPUT_INVALID);
    if (kind.equals("GROUP")) {
      AppException.require(
          requestValues.keySet().equals(Set.of("groupId", "name", "active"))
              && requestValues.get("groupId").matches("[A-Za-z0-9_-]{2,60}")
              && !requestValues.get("name").isBlank()
              && Set.of("true", "false").contains(requestValues.get("active")),
          IdentityErrorCatalog.INPUT_INVALID);
      return;
    }
    AppException.require(
        Set.of("ADD", "REMOVE").contains(requestValues.getOrDefault("operation", "")),
        IdentityErrorCatalog.INPUT_INVALID);
    if (kind.equals("GRANT") && requestValues.get("operation").equals("REMOVE")) {
      AppException.require(
          requestValues.keySet().equals(Set.of("operation", "grantId")),
          IdentityErrorCatalog.INPUT_INVALID);
      return;
    }
    AppException.require(
        groups.existsById(requestValues.getOrDefault("groupId", "")),
        IdentityErrorCatalog.INPUT_INVALID);
    if (kind.equals("MEMBERSHIP")) {
      AppException.require(
          requestValues.keySet().equals(Set.of("userId", "groupId", "operation"))
              && users.existsById(requestValues.get("userId")),
          IdentityErrorCatalog.INPUT_INVALID);
    } else {
      AppException.require(
          (requestValues
                      .keySet()
                      .equals(Set.of("groupId", "feature", "action", "scope", "operation"))
                  || requestValues
                      .keySet()
                      .equals(
                          Set.of(
                              "groupId",
                              "feature",
                              "action",
                              "scope",
                              "operation",
                              "hiddenFields")))
              && !requestValues.get("feature").isBlank()
              && !requestValues.get("scope").isBlank(),
          IdentityErrorCatalog.INPUT_INVALID);
      try {
        Action.valueOf(requestValues.get("action"));
      } catch (Exception exception) {
        throw AppException.of(IdentityErrorCatalog.INPUT_INVALID);
      }
    }
  }

  @Override
  public void seed(String password, PasswordEncoder encoder) {
    if (users.count() > 0) return;
    for (String id : List.of("maker", "checker", "reviewer", "other")) {
      UserAccount userAccount = new UserAccount();
      userAccount.setId(id);
      userAccount.setDisplayName(
          switch (id) {
            case "maker" -> "Maya Tan";
            case "checker" -> "James Lim";
            case "reviewer" -> "Asha Shah";
            default -> "Other group user";
          });
      userAccount.setPasswordHash(encoder.encode(password));
      userAccount.setActive(true);
      users.save(userAccount);
    }
    for (String id : List.of("bullion", "other", "csa")) {
      UserGroup userGroup = new UserGroup();
      userGroup.setId(id);
      userGroup.setName(
          id.equals("bullion")
              ? "Bullion Clearing"
              : id.equals("csa") ? "R2WD_GTO_ISTOA_CSA" : "Other Operations");
      userGroup.setActive(true);
      groups.save(userGroup);
    }
    for (String userId : List.of("maker", "checker", "reviewer", "other")) {
      Membership membership = new Membership();
      membership.setId(Ids.next());
      membership.setUserId(userId);
      membership.setGroupId(userId.equals("other") ? "other" : "bullion");
      members.save(membership);
    }
    for (String userId : List.of("maker", "checker")) {
      Membership membership = new Membership();
      membership.setId(Ids.next());
      membership.setUserId(userId);
      membership.setGroupId("csa");
      members.save(membership);
    }
    for (String groupId : List.of("bullion", "other"))
      for (Action action : Action.values()) {
        if (action == Action.ADMIN) continue;
        PermissionGrant permissionGrant = new PermissionGrant();
        permissionGrant.setId(Ids.next());
        permissionGrant.setGroupId(groupId);
        permissionGrant.setFeature("*");
        permissionGrant.setAction(action.name());
        permissionGrant.setScope(groupId.equals("bullion") ? "BULLION" : "OTHER");
        grants.save(permissionGrant);
      }
    for (Action action : Action.values()) {
      PermissionGrant permissionGrant = new PermissionGrant();
      permissionGrant.setId(Ids.next());
      permissionGrant.setGroupId("csa");
      permissionGrant.setFeature("administration");
      permissionGrant.setAction(action.name());
      permissionGrant.setScope("SYSTEM");
      grants.save(permissionGrant);
    }
  }
}
