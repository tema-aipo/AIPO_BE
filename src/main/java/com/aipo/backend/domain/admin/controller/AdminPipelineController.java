package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.pipeline.entity.PipelineJob;
import com.aipo.backend.domain.pipeline.entity.PipelineJobStatus;
import com.aipo.backend.domain.pipeline.repository.PipelineJobRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/pipeline")
@RequiredArgsConstructor
public class AdminPipelineController {

    private final PipelineJobRepository pipelineJobRepository;

    @GetMapping("/jobs")
    public Page<PipelineJobResponse> listJobs(
            @RequestParam(required = false) PipelineJobStatus status,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return pipelineJobRepository.findAllByFilter(status, jobType, pageable)
                .map(PipelineJobResponse::from);
    }

    @GetMapping("/jobs/{jobId}")
    public PipelineJobResponse getJob(@PathVariable Long jobId) {
        PipelineJob job = pipelineJobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PIPELINE_JOB_NOT_FOUND));
        return PipelineJobResponse.from(job);
    }

    @GetMapping("/status")
    public PipelineSummaryResponse summary() {
        long queued = pipelineJobRepository.countByJobStatus(PipelineJobStatus.QUEUED);
        long running = pipelineJobRepository.countByJobStatus(PipelineJobStatus.RUNNING);
        long completed = pipelineJobRepository.countByJobStatus(PipelineJobStatus.COMPLETED);
        long failed = pipelineJobRepository.countByJobStatus(PipelineJobStatus.FAILED);
        long cancelled = pipelineJobRepository.countByJobStatus(PipelineJobStatus.CANCELLED);
        return new PipelineSummaryResponse(queued, running, completed, failed, cancelled);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<PipelineJobResponse> cancelJob(@PathVariable Long jobId) {
        PipelineJob job = pipelineJobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PIPELINE_JOB_NOT_FOUND));

        if (job.getJobStatus() != PipelineJobStatus.QUEUED && job.getJobStatus() != PipelineJobStatus.RUNNING) {
            throw new CustomException(ErrorCode.PIPELINE_JOB_CANCEL_DENIED,
                    "취소할 수 없는 상태입니다: " + job.getJobStatus());
        }
        job.cancel();
        pipelineJobRepository.save(job);
        return ResponseEntity.ok(PipelineJobResponse.from(job));
    }

    @Getter
    @AllArgsConstructor
    static class PipelineJobResponse {
        private Long jobId;
        private Long docId;
        private String originalDocName;
        private String jobType;
        private PipelineJobStatus jobStatus;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;
        private LocalDateTime createdAt;

        static PipelineJobResponse from(PipelineJob job) {
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
    static class PipelineSummaryResponse {
        private long queued;
        private long running;
        private long completed;
        private long failed;
        private long cancelled;
    }
}
