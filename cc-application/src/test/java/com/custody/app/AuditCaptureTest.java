package com.custody.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.custody.audit.service.impl.AuditCaptureImpl;
import com.custody.core.audit.spi.AuditEventAdapter;
import com.custody.core.dto.AuditDraft;
import com.custody.core.enums.AuditCategory;
import com.custody.core.error.CoreErrorCatalog;
import com.custody.core.exception.AppException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditCaptureTest {
  private AuditDraft draft(String reference) {
    return AuditDraft.builder(AuditCategory.BUSINESS)
        .feature("inventory")
        .scope("BULLION")
        .recordId(reference)
        .reference(reference)
        .action("RECONCILE")
        .status("COMPLETED")
        .actor("maker")
        .maker("maker")
        .reason("Reconciliation")
        .source("Inventory")
        .before(Map.of())
        .after(Map.of())
        .build();
  }

  private AuditEventAdapter<String> adapter() {
    return new AuditEventAdapter<>() {
      public Class<String> eventType() {
        return String.class;
      }

      public AuditDraft toAuditDraft(String event) {
        return draft(event);
      }
    };
  }

  @Test
  void aRegisteredFeatureIsDispatchedWithoutChangingCentralCode() {
    List<AuditDraft> recordedDrafts = new ArrayList<>();
    var capture = new AuditCaptureImpl(List.of(adapter()), recordedDrafts::add);
    capture.record("inventory-reference");
    assertThat(recordedDrafts).hasSize(1);
    assertThat(recordedDrafts.getFirst().feature()).isEqualTo("inventory");
    assertThat(recordedDrafts.getFirst().reference()).isEqualTo("inventory-reference");
  }

  @Test
  void duplicateRegistrationsFailInsteadOfChoosingAnArbitraryAdapter() {
    assertThatThrownBy(() -> new AuditCaptureImpl(List.of(adapter(), adapter()), event -> {}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate audit adapter");
  }

  @Test
  void unregisteredAndNullEventsNeverReachPersistence() {
    List<AuditDraft> recordedDrafts = new ArrayList<>();
    var capture = new AuditCaptureImpl(List.of(adapter()), recordedDrafts::add);
    assertThatThrownBy(() -> capture.record(123))
        .isInstanceOfSatisfying(
            AppException.class,
            exception ->
                assertThat(exception.code())
                    .isEqualTo(CoreErrorCatalog.AUDIT_EVIDENCE_INVALID.code()));
    assertThatThrownBy(() -> capture.record(null)).isInstanceOf(AppException.class);
    assertThat(recordedDrafts).isEmpty();
  }

  @Test
  void adapterFailuresAndMissingDraftsDoNotSilentlyDropEvidence() {
    AuditEventAdapter<String> missingDraftAdapter =
        new AuditEventAdapter<>() {
          public Class<String> eventType() {
            return String.class;
          }

          public AuditDraft toAuditDraft(String event) {
            return null;
          }
        };
    List<AuditDraft> recordedDrafts = new ArrayList<>();
    var capture = new AuditCaptureImpl(List.of(missingDraftAdapter), recordedDrafts::add);
    assertThatThrownBy(() -> capture.record("reference")).isInstanceOf(AppException.class);
    assertThat(recordedDrafts).isEmpty();
    AuditEventAdapter<String> failingAdapter =
        new AuditEventAdapter<>() {
          public Class<String> eventType() {
            return String.class;
          }

          public AuditDraft toAuditDraft(String event) {
            throw new IllegalStateException("Mapping failed");
          }
        };
    var failingCapture = new AuditCaptureImpl(List.of(failingAdapter), recordedDrafts::add);
    assertThatThrownBy(() -> failingCapture.record("reference"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(recordedDrafts).isEmpty();
  }
}
