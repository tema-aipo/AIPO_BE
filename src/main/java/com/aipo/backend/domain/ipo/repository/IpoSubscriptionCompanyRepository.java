package com.aipo.backend.domain.ipo.repository;

import com.aipo.backend.domain.ipo.entity.IpoSubscriptionCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpoSubscriptionCompanyRepository extends JpaRepository<IpoSubscriptionCompany, Long> {

    List<IpoSubscriptionCompany> findAllByStock_IdOrderByDisplayOrderAsc(Long stockId);
}
