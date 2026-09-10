package com.custody.audit.repository;

import com.custody.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditRepository
    extends JpaRepository<AuditEvent, String>, JpaSpecificationExecutor<AuditEvent> {}
