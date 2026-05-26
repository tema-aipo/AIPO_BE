package com.aipo.backend.domain.investmentprofile.repository;

import com.aipo.backend.domain.investmentprofile.entity.UserInvestmentProfileResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserInvestmentProfileResultRepository extends JpaRepository<UserInvestmentProfileResult, Long> {

    Optional<UserInvestmentProfileResult> findByUserIdAndCurrentTrue(Long userId);

    Optional<UserInvestmentProfileResult> findTopByUserIdAndCurrentTrueOrderByCreatedAtDescIdDesc(Long userId);

    @Query(value = """
            select profile_type
              from user_investment_profile_result
             where user_id = :userId
               and is_current = 1
             order by created_at desc, result_id desc
             limit 1
            """, nativeQuery = true)
    Optional<String> findCurrentProfileTypeValueByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndCurrentTrue(Long userId);

    @Modifying
    @Query("""
            update UserInvestmentProfileResult result
               set result.current = false
             where result.userId = :userId
               and result.current = true
            """)
    int clearCurrentResult(@Param("userId") Long userId);
}
