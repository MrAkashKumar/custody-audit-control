package com.custody.workflow.repository;

import com.custody.workflow.model.SetupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SetupRecordRepository
    extends JpaRepository<SetupRecord, String>, JpaSpecificationExecutor<SetupRecord> {}
