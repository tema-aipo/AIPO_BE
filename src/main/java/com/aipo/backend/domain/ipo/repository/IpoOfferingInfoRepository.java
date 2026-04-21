package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoOfferingInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpoOfferingInfoRepository extends JpaRepository<IpoOfferingInfo, Long> {

    Optional<IpoOfferingInfo> findByStock_Id(Long stockId);
}
