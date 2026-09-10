package com.custody.reporting.repository;

import com.custody.reporting.model.ReportArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportArtifactRepository extends JpaRepository<ReportArtifact, String> {}
