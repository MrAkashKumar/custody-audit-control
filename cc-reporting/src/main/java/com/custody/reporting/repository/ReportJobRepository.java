package com.custody.reporting.repository;

import com.custody.reporting.model.ReportJob;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ReportJobRepository extends JpaRepository<ReportJob, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ReportJob r where r.id=:id")
  Optional<ReportJob> lock(String id);

  @Query(
      "select r from ReportJob r where r.status=com.custody.reporting.enums.ReportStatus.REQUESTED or (r.status=com.custody.reporting.enums.ReportStatus.RUNNING and r.claimedAt < :cutoff) order by r.requestedAt")
  List<ReportJob> available(
      java.time.Instant cutoff, org.springframework.data.domain.Pageable page);
}
