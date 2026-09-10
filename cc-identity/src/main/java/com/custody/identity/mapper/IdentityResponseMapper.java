package com.custody.identity.mapper;

import com.custody.core.utils.JsonValues;
import com.custody.identity.dto.response.SecurityChangeResponse;
import com.custody.identity.model.SecurityChange;
import org.springframework.stereotype.Component;

@Component
public class IdentityResponseMapper {
  private final JsonValues jsonValues;

  public IdentityResponseMapper(JsonValues jsonValues) {
    this.jsonValues = jsonValues;
  }

  /** Maps a persisted security change and its stored payload to the response DTO. */
  public SecurityChangeResponse toSecurityChangeResponse(SecurityChange securityChangeEntity) {
    return new SecurityChangeResponse(
        securityChangeEntity.getId(),
        securityChangeEntity.getKind(),
        jsonValues.read(securityChangeEntity.getPayload()),
        securityChangeEntity.getMaker(),
        securityChangeEntity.getChecker(),
        securityChangeEntity.getReason(),
        securityChangeEntity.getStatus(),
        securityChangeEntity.getSubmittedAt());
  }
}
