package com.aipo.backend.domain.admin.service;

import com.aipo.backend.domain.admin.controller.AdminPipelineController.PipelineJobResponse;
import com.aipo.backend.domain.admin.controller.AdminPipelineController.PipelineSummaryResponse;
import com.aipo.backend.domain.pipeline.entity.PipelineJob;
import com.aipo.backend.domain.pipeline.entity.PipelineJobStatus;
import com.aipo.backend.domain.pipeline.repository.PipelineJobRepository;
import com.aipo.backend.global.exception.CustomException;
import com.aipo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PipelineAdminService {

    private final PipelineJobRepository pipelineJobRepository;

    public Page<PipelineJobResponse> listJobs(PipelineJobStatus status, String jobType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return pipelineJobRepository.findAllByFilter(status, jobType, pageable)
                .map(PipelineJobResponse::from);
    }

    public PipelineJobResponse getJob(Long jobId) {
        PipelineJob job = pipelineJobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PIPELINE_JOB_NOT_FOUND));
        return PipelineJobResponse.from(job);
    }

    public PipelineSummaryResponse getSummary() {
        long queued = pipelineJobRepository.countByJobStatus(PipelineJobStatus.QUEUED);
        long running = pipelineJobRepository.countByJobStatus(PipelineJobStatus.RUNNING);
        long completed = pipelineJobRepository.countByJobStatus(PipelineJobStatus.COMPLETED);
        long failed = pipelineJobRepository.countByJobStatus(PipelineJobStatus.FAILED);
        long cancelled = pipelineJobRepository.countByJobStatus(PipelineJobStatus.CANCELLED);
        return new PipelineSummaryResponse(queued, running, completed, failed, cancelled);
    }

    @Transactional
    public PipelineJobResponse cancelJob(Long jobId) {
        PipelineJob job = pipelineJobRepository.findById(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PIPELINE_JOB_NOT_FOUND));

        if (job.getJobStatus() != PipelineJobStatus.QUEUED && job.getJobStatus() != PipelineJobStatus.RUNNING) {
            throw new CustomException(ErrorCode.PIPELINE_JOB_CANCEL_DENIED,
                    "취소할 수 없는 상태입니다: " + job.getJobStatus());
        }

        job.cancel();
        return PipelineJobResponse.from(job);
    }
}