package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoAttractionReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpoAttractionReasonRepository extends JpaRepository<IpoAttractionReason, Long> {

    List<IpoAttractionReason> findAllByStock_IdOrderByDisplayOrderAsc(Long stockId);
}
