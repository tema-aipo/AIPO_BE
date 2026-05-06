package com.aipo.backend.domain.external.common.repository;

import com.aipo.backend.domain.external.common.entity.IpoExternalSourceData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IpoExternalSourceDataRepository extends JpaRepository<IpoExternalSourceData, Long> {

    Optional<IpoExternalSourceData> findByProviderAndSourceTypeAndExternalKey(
            String provider,
            String sourceType,
            String externalKey
    );

    List<IpoExternalSourceData> findAllByDartCorpCode(String dartCorpCode);

    List<IpoExternalSourceData> findAllByCorpName(String corpName);
}
