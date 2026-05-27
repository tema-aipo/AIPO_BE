package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.admin.service.PipelineAdminService;
import com.aipo.backend.domain.pipeline.entity.PipelineJob;
import com.aipo.backend.domain.pipeline.entity.PipelineJobStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "관리자 - 파이프라인", description = "파이프라인 작업 조회 및 취소")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/pipeline")
@RequiredArgsConstructor
public class AdminPipelineController {

    private final PipelineAdminService pipelineAdminService; // ✨ 수정된 서비스 이름 적용

    @Operation(summary = "파이프라인 작업 목록 조회")
    @GetMapping("/jobs")
    public Page<PipelineJobResponse> listJobs(
            @RequestParam(required = false) PipelineJobStatus status,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return pipelineAdminService.listJobs(status, jobType, page, size);
    }

    @Operation(summary = "파이프라인 작업 상세 조회")
    @GetMapping("/jobs/{jobId}")
    public PipelineJobResponse getJob(@PathVariable Long jobId) {
        return pipelineAdminService.getJob(jobId);
    }

    @Operation(summary = "파이프라인 상태 요약")
    @GetMapping("/status")
    public PipelineSummaryResponse summary() {
        return pipelineAdminService.getSummary();
    }

    @Operation(summary = "파이프라인 작업 취소")
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<PipelineJobResponse> cancelJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(pipelineAdminService.cancelJob(jobId));
    }

    @Getter
    @AllArgsConstructor
    public static class PipelineJobResponse {
        private Long jobId;
        private Long docId;
        private String originalDocName;
        private String jobType;
        private PipelineJobStatus jobStatus;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;
        private LocalDateTime createdAt;

        public static PipelineJobResponse from(PipelineJob job) {
            return new PipelineJobResponse(
                    job.getJobId(),
                    job.getDocument() != null ? job.getDocument().getDocId() : null,
                    job.getDocument() != null ? job.getDocument().getOriginalName() : null,
                    job.getJobType(),
                    job.getJobStatus(),
                    job.getStartedAt(),
                    job.getCompletedAt(),
                    job.getErrorMessage(),
                    job.getCreatedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PipelineSummaryResponse {
        private long queued;
        private long running;
        private long completed;
        private long failed;
        private long cancelled;
    }
}