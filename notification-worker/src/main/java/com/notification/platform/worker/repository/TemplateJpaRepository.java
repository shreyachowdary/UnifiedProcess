package com.notification.platform.worker.repository;

import com.notification.platform.worker.model.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateJpaRepository extends JpaRepository<TemplateEntity, Long> {

    Optional<TemplateEntity> findByTemplateId(String templateId);
}
