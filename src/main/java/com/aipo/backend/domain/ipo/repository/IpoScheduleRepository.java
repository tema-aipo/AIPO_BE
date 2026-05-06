package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import com.aipo.backend.domain.ipo.entity.ScheduleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IpoScheduleRepository extends JpaRepository<IpoSchedule, Long> {

    List<IpoSchedule> findAllByStock_Id(Long stockId);

    Optional<IpoSchedule> findByStock_IdAndScheduleType(Long stockId, ScheduleType scheduleType);

    @EntityGraph(attributePaths = "stock")
    List<IpoSchedule> findAllByScheduleDateBetweenOrderByScheduleDateAsc(LocalDate startDate, LocalDate endDate);
}
