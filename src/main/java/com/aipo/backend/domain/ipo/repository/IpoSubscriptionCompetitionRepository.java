package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoSubscriptionCompetition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpoSubscriptionCompetitionRepository extends JpaRepository<IpoSubscriptionCompetition, Long> {

    Optional<IpoSubscriptionCompetition> findByStock_Id(Long stockId);
}
