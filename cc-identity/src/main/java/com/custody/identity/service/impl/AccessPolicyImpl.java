package com.custody.identity.service.impl;

import com.custody.core.enums.Action;
import com.custody.core.exception.AppException;
import com.custody.identity.dto.ScopedGrant;
import com.custody.identity.error.IdentityErrorCatalog;
import com.custody.identity.repository.MembershipRepository;
import com.custody.identity.repository.PermissionGrantRepository;
import com.custody.identity.repository.UserAccountRepository;
import com.custody.identity.repository.UserGroupRepository;
import com.custody.identity.service.AccessPolicy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessPolicyImpl implements AccessPolicy {
  private final UserAccountRepository users;
  private final UserGroupRepository groups;
  private final MembershipRepository members;
  private final PermissionGrantRepository grants;

  public AccessPolicyImpl(
      UserAccountRepository userAccountRepository,
      UserGroupRepository userGroupRepository,
      MembershipRepository membershipRepository,
      PermissionGrantRepository permissionGrantRepository) {
    this.users = userAccountRepository;
    this.groups = userGroupRepository;
    this.members = membershipRepository;
    this.grants = permissionGrantRepository;
  }

  @Override
  public List<ScopedGrant> grants(String actor, Action action) {
    if (users.findById(actor).filter(userAccount -> userAccount.getActive()).isEmpty())
      return List.of();
    Set<String> active = new HashSet<>();
    groups.findAll().stream()
        .filter(userGroup -> userGroup.getActive())
        .forEach(userGroup -> active.add(userGroup.getId()));
    Set<String> joined = new HashSet<>();
    members.findAll().stream()
        .filter(
            membership ->
                membership.getUserId().equals(actor) && active.contains(membership.getGroupId()))
        .forEach(membership -> joined.add(membership.getGroupId()));
    return grants.findAll().stream()
        .filter(
            permissionGrant ->
                joined.contains(permissionGrant.getGroupId())
                    && permissionGrant.getAction().equals(action.name()))
        .map(
            permissionGrant ->
                new ScopedGrant(permissionGrant.getFeature(), permissionGrant.getScope()))
        .toList();
  }

  @Override
  public Set<String> scopes(String actor, String feature, Action action) {
    if (users.findById(actor).filter(userAccount -> userAccount.getActive()).isEmpty())
      return Set.of();
    Set<String> activeGroups = new HashSet<>();
    groups.findAll().stream()
        .filter(userGroup -> userGroup.getActive())
        .forEach(userGroup -> activeGroups.add(userGroup.getId()));
    Set<String> joined = new HashSet<>();
    members.findAll().stream()
        .filter(
            membership ->
                membership.getUserId().equals(actor)
                    && activeGroups.contains(membership.getGroupId()))
        .forEach(membership -> joined.add(membership.getGroupId()));
    Set<String> result = new HashSet<>();
    grants.findAll().stream()
        .filter(
            permissionGrant ->
                joined.contains(permissionGrant.getGroupId())
                    && permissionGrant.getAction().equals(action.name())
                    && (permissionGrant.getFeature().equals(feature)
                        || permissionGrant.getFeature().equals("*")))
        .forEach(permissionGrant -> result.add(permissionGrant.getScope()));
    return Set.copyOf(result);
  }

  @Override
  public boolean allows(String actor, String feature, Action action, String scope) {
    return scopes(actor, feature, action).contains(scope);
  }

  @Override
  public void require(String actor, String feature, Action action, String scope) {
    AppException.require(
        allows(actor, feature, action, scope), IdentityErrorCatalog.ACTION_FORBIDDEN);
  }

  @Override
  public Set<String> hiddenFields(String actor, String feature, Action action, String scope) {
    Set<String> joined = new HashSet<>();
    Set<String> active = new HashSet<>();
    groups.findAll().stream()
        .filter(userGroup -> userGroup.getActive())
        .forEach(userGroup -> active.add(userGroup.getId()));
    members.findAll().stream()
        .filter(
            membership ->
                membership.getUserId().equals(actor) && active.contains(membership.getGroupId()))
        .forEach(membership -> joined.add(membership.getGroupId()));
    Set<String> hidden = new HashSet<>();
    grants.findAll().stream()
        .filter(
            permissionGrant ->
                joined.contains(permissionGrant.getGroupId())
                    && permissionGrant.getAction().equals(action.name())
                    && permissionGrant.getScope().equals(scope)
                    && (permissionGrant.getFeature().equals(feature)
                        || permissionGrant.getFeature().equals("*"))
                    && permissionGrant.getHiddenFields() != null)
        .forEach(
            permissionGrant ->
                Arrays.stream(permissionGrant.getHiddenFields().split(","))
                    .map(String::trim)
                    .filter(hiddenFieldName -> !hiddenFieldName.isEmpty())
                    .forEach(hidden::add));
    return Set.copyOf(hidden);
  }

  @Override
  public void administration(String actor) {
    require(actor, "administration", Action.ADMIN, "SYSTEM");
  }
}
