package com.custody.reporting.repository;

import com.custody.reporting.model.ReportSchedule;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ReportSchedule s where s.id=:id")
  Optional<ReportSchedule> lock(String id);
}
