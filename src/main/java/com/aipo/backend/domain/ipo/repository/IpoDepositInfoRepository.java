package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoDepositInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpoDepositInfoRepository extends JpaRepository<IpoDepositInfo, Long> {

    List<IpoDepositInfo> findAllByStock_IdOrderByDisplayOrderAsc(Long stockId);
}
