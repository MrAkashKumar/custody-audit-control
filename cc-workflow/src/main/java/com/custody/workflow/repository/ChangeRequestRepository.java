package com.custody.workflow.repository;

import com.custody.workflow.model.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChangeRequestRepository
    extends JpaRepository<ChangeRequest, String>, JpaSpecificationExecutor<ChangeRequest> {}
