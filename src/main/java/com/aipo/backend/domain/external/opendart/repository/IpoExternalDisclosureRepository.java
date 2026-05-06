package com.aipo.backend.domain.external.opendart.repository;

import com.aipo.backend.domain.external.opendart.entity.IpoExternalDisclosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IpoExternalDisclosureRepository extends JpaRepository<IpoExternalDisclosure, Long> {

    boolean existsByRceptNo(String rceptNo);

    Optional<IpoExternalDisclosure> findByRceptNo(String rceptNo);

    List<IpoExternalDisclosure> findAllByCorpCodeInAndProcessedFalse(Collection<String> corpCodes);
}
