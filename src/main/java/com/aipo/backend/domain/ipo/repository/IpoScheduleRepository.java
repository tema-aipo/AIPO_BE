package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IpoScheduleRepository extends JpaRepository<IpoSchedule, Long> {

    List<IpoSchedule> findAllByStock_Id(Long stockId);

    @EntityGraph(attributePaths = "stock")
    List<IpoSchedule> findAllByScheduleDateBetweenOrderByScheduleDateAsc(LocalDate startDate, LocalDate endDate);
}
