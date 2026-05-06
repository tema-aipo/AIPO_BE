package com.aipo.backend.domain.external.opendart.repository;

import com.aipo.backend.domain.external.opendart.entity.IpoExternalCompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IpoExternalCompanyProfileRepository extends JpaRepository<IpoExternalCompanyProfile, Long> {

    Optional<IpoExternalCompanyProfile> findByProviderAndDartCorpCode(String provider, String dartCorpCode);
}
