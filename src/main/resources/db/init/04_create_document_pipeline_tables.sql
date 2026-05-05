-- =============================================
-- 문서 테이블
-- 관리자가 업로드한 PDF 등 문서 메타데이터
-- doc_status: UPLOADED | PROCESSING | COMPLETED | FAILED
-- =============================================
CREATE TABLE document (
    doc_id          BIGINT GENERATED ALWAYS AS IDENTITY,
    uploader_id     BIGINT NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    stored_name     VARCHAR(255) NOT NULL,
    upload_path     VARCHAR(500) NOT NULL,
    file_size       BIGINT NOT NULL,
    content_type    VARCHAR(100),
    doc_status      VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    uploaded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (doc_id),
    CONSTRAINT fk_document_uploader
        FOREIGN KEY (uploader_id) REFERENCES app_user(user_id) ON DELETE RESTRICT
);

CREATE INDEX idx_document_uploaded_at ON document(uploaded_at DESC);

-- =============================================
-- 파이프라인 작업 테이블
-- 문서 처리 파이프라인 작업 상태 추적
-- job_status: QUEUED | RUNNING | COMPLETED | FAILED | CANCELLED
-- job_type 예시: PDF_PARSE, DART_CRAWL 등
-- =============================================
CREATE TABLE pipeline_job (
    job_id          BIGINT GENERATED ALWAYS AS IDENTITY,
    doc_id          BIGINT,
    job_type        VARCHAR(50) NOT NULL,
    job_status      VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    started_at      TIMESTAMP NULL,
    completed_at    TIMESTAMP NULL,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (job_id),
    CONSTRAINT fk_pipeline_job_document
        FOREIGN KEY (doc_id) REFERENCES document(doc_id) ON DELETE SET NULL
);

CREATE INDEX idx_pipeline_job_doc_id     ON pipeline_job(doc_id);
CREATE INDEX idx_pipeline_job_status     ON pipeline_job(job_status);
CREATE INDEX idx_pipeline_job_created_at ON pipeline_job(created_at DESC);
