package com.notification.platform.api.repository;

import com.notification.platform.api.model.sql.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByTemplateId(String templateId);
}
