package com.aipo.backend.domain.document.entity;

import com.aipo.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document",
        indexes = @Index(name = "idx_document_uploaded_at", columnList = "uploaded_at DESC"))
@Getter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "upload_path", nullable = false, length = 500)
    private String uploadPath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_status", nullable = false, length = 20)
    private DocumentStatus docStatus;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Document(User uploader, String originalName, String storedName,
                    String uploadPath, Long fileSize, String contentType) {
        this.uploader = uploader;
        this.originalName = originalName;
        this.storedName = storedName;
        this.uploadPath = uploadPath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.docStatus = DocumentStatus.UPLOADED;
        this.uploadedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(DocumentStatus status) {
        this.docStatus = status;
        this.updatedAt = LocalDateTime.now();
    }
}
