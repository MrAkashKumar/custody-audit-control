package com.custody.identity.repository;

import com.custody.identity.model.SecurityChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityChangeRepository extends JpaRepository<SecurityChange, String> {}
