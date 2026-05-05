package com.aipo.backend.domain.admin.controller;

import com.aipo.backend.domain.document.entity.DocumentStatus;
import com.aipo.backend.domain.document.service.DocumentService;
import com.aipo.backend.domain.user.entity.User;
import com.aipo.backend.domain.user.repository.UserRepository;
import com.aipo.backend.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentService.DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        User uploader = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        DocumentService.DocumentResponse response = documentService.upload(uploader, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public Page<DocumentService.DocumentResponse> listDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        return documentService.listDocuments(status, keyword, pageable);
    }

    @GetMapping("/{docId}")
    public DocumentService.DocumentResponse getDocument(@PathVariable Long docId) {
        return documentService.getDocument(docId);
    }
}
