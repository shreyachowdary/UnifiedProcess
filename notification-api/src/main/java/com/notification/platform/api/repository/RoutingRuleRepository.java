package com.notification.platform.api.repository;

import com.notification.platform.api.model.sql.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, Long> {

    List<RoutingRule> findByClientIdAndChannelAndEnabledTrueOrderByPriorityAsc(String clientId, String channel);
}
