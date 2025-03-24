package org.zhou.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zhou.backend.entity.BonusRule;

import java.util.List;

@Repository
public interface BonusRuleRepository extends JpaRepository<BonusRule, Long> {
    List<BonusRule> findByType(String type);
    List<BonusRule> findByTypeAndActivityType(String type, String activityType);
}
