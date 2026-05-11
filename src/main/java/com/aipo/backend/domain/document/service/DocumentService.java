package com.aipo.backend.domain.document.service;

import com.aipo.backend.domain.document.entity.Document;
import com.aipo.backend.domain.document.entity.DocumentStatus;
import com.aipo.backend.domain.document.repository.DocumentRepository;
import com.aipo.backend.domain.pipeline.entity.PipelineJob;
import com.aipo.backend.domain.pipeline.repository.PipelineJobRepository;
import com.aipo.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PipelineJobRepository pipelineJobRepository;

    @Value("${upload.dir}")
    private String uploadDir;

    public DocumentResponse upload(User uploader, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(storedName);
        file.transferTo(filePath);

        Document document = new Document(
                uploader,
                originalName != null ? originalName : storedName,
                storedName,
                filePath.toString(),
                file.getSize(),
                file.getContentType()
        );
        Document saved = documentRepository.save(document);

        PipelineJob job = new PipelineJob(saved, "PDF_PARSE");
        pipelineJobRepository.save(job);

        return DocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(DocumentStatus status, String keyword, Pageable pageable) {
        return documentRepository.findAllByFilter(status, keyword, pageable)
                .map(DocumentResponse::from);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long docId) {
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new com.aipo.backend.global.exception.CustomException(
                        com.aipo.backend.global.exception.ErrorCode.DOCUMENT_NOT_FOUND));
        return DocumentResponse.from(document);
    }

    @Getter
    @AllArgsConstructor
    public static class DocumentResponse {
        private Long docId;
        private String originalName;
        private Long fileSize;
        private String contentType;
        private DocumentStatus docStatus;
        private String uploaderLoginId;
        private LocalDateTime uploadedAt;

        public static DocumentResponse from(Document doc) {
            return new DocumentResponse(
                    doc.getDocId(),
                    doc.getOriginalName(),
                    doc.getFileSize(),
                    doc.getContentType(),
                    doc.getDocStatus(),
                    doc.getUploader().getLoginId(),
                    doc.getUploadedAt()
            );
        }
    }
}
