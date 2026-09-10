package com.custody.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.custody.core.builder.AuditDraftBuilder;
import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.AuditCategory;
import com.custody.core.error.CoreErrorCatalog;
import com.custody.core.exception.AppException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditDraftTest {
  private AuditDraftBuilder validDraft() {
    return AuditDraft.builder(AuditCategory.STATIC)
        .feature("vault")
        .scope("BULLION")
        .recordId("VAULT-1")
        .reference("CHANGE-1")
        .action("EDIT")
        .status("PENDING")
        .actor("maker")
        .maker("maker")
        .reason("Correct location")
        .source("Vault")
        .before(Map.of("location", "London"))
        .proposed(Map.of("location", "Singapore"))
        .after(Map.of("location", "Singapore"));
  }

  @Test
  void snapshotsAreDefensivelyCopiedAndImmutable() {
    Map<String, String> values = new HashMap<>(Map.of("location", "Singapore"));
    AuditDraft draft = validDraft().after(values).build();
    values.put("location", "London");
    assertThat(draft.after()).containsEntry("location", "Singapore");
    assertThat(draft.proposed()).containsEntry("location", "Singapore");
    assertThatThrownBy(() -> draft.after().put("location", "London"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void incompleteEvidenceHasStableInternalErrorCode() {
    assertThatThrownBy(() -> validDraft().actor(" ").build())
        .isInstanceOfSatisfying(
            AppException.class,
            exception ->
                assertThat(exception.code())
                    .isEqualTo(CoreErrorCatalog.AUDIT_EVIDENCE_INVALID.code()));
    assertThatThrownBy(() -> validDraft().after(null).build()).isInstanceOf(AppException.class);
    assertThatThrownBy(() -> AuditDraft.builder(null).build()).isInstanceOf(AppException.class);
  }

  @Test
  void snapshotsRejectNullValuesWithTheSameSafeError() {
    Map<String, String> values = new HashMap<>();
    values.put("location", null);
    assertThatThrownBy(() -> validDraft().after(values).build())
        .isInstanceOfSatisfying(
            AppException.class,
            exception ->
                assertThat(exception.code())
                    .isEqualTo(CoreErrorCatalog.AUDIT_EVIDENCE_INVALID.code()));
  }

  @Test
  void activitiesCanHaveEmptySnapshotsAndNoChecker() {
    AuditDraft draft = validDraft().before(Map.of()).proposed(Map.of()).after(Map.of()).build();
    assertThat(draft.before()).isEmpty();
    assertThat(draft.proposed()).isEmpty();
    assertThat(draft.after()).isEmpty();
    assertThat(draft.checker()).isEmpty();
  }
}
