package org.zhou.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.ImportLog;

@Repository
public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {
    List<ImportLog> findTop50ByOrderByTimeDesc();
} 