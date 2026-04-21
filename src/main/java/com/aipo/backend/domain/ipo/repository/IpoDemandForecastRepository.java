package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoDemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpoDemandForecastRepository extends JpaRepository<IpoDemandForecast, Long> {

    Optional<IpoDemandForecast> findByStock_Id(Long stockId);
}
