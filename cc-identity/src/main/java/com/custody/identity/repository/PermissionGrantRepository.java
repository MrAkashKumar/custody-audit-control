package com.custody.identity.repository;

import com.custody.identity.model.PermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGrantRepository extends JpaRepository<PermissionGrant, String> {}
