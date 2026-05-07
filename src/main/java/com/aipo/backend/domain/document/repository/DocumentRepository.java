package com.aipo.backend.domain.document.repository;

import com.aipo.backend.domain.document.entity.Document;
import com.aipo.backend.domain.document.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT d FROM Document d WHERE " +
           "(:status IS NULL OR d.docStatus = :status) AND " +
           "(:keyword IS NULL OR LOWER(d.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> findAllByFilter(
            @Param("status") DocumentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByDocStatus(DocumentStatus docStatus);

    boolean existsByExternalId(String externalId);
}
