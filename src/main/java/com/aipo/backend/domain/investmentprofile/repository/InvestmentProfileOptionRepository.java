package com.aipo.backend.domain.investmentprofile.repository;

import com.aipo.backend.domain.investmentprofile.entity.InvestmentProfileOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InvestmentProfileOptionRepository extends JpaRepository<InvestmentProfileOption, Long> {

    List<InvestmentProfileOption> findAllByQuestion_IdInOrderByQuestion_QuestionOrderAscOptionOrderAsc(Collection<Long> questionIds);
}
