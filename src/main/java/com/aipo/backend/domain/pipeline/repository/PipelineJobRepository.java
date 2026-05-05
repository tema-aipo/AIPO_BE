package com.aipo.backend.domain.pipeline.repository;

import com.aipo.backend.domain.pipeline.entity.PipelineJob;
import com.aipo.backend.domain.pipeline.entity.PipelineJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineJobRepository extends JpaRepository<PipelineJob, Long> {

    @Query("SELECT p FROM PipelineJob p WHERE " +
           "(:status IS NULL OR p.jobStatus = :status) AND " +
           "(:jobType IS NULL OR p.jobType = :jobType)")
    Page<PipelineJob> findAllByFilter(
            @Param("status") PipelineJobStatus status,
            @Param("jobType") String jobType,
            Pageable pageable);

    long countByJobStatus(PipelineJobStatus jobStatus);
}
