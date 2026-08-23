package com.mindq.material.controller;

import com.mindq.common.api.ApiResponse;
import com.mindq.common.api.PaginatedResponse;
import com.mindq.material.dto.CreateMaterialRequest;
import com.mindq.material.dto.MaterialDetailResponse;
import com.mindq.material.dto.MaterialSummaryResponse;
import com.mindq.material.dto.UpdateMaterialRequest;
import com.mindq.material.service.MaterialService;
import com.mindq.material.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialDetailResponse>> create(
            @Valid @RequestBody CreateMaterialRequest request,
            Authentication authentication) {
        MaterialDetailResponse material = materialService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(material, "Material created successfully"));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MaterialDetailResponse>> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            Authentication authentication) {
        MaterialDetailResponse material = materialService.uploadFile(authentication.getName(), title, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(material, "Material created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<MaterialSummaryResponse>>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PaginatedResponse<MaterialSummaryResponse> materials = materialService.list(authentication.getName(), page, size, search);
        return ResponseEntity.ok(ApiResponse.success(materials, "Materials retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialDetailResponse>> get(
            @PathVariable Long id,
            Authentication authentication) {
        MaterialDetailResponse material = materialService.get(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(material, "Material retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialDetailResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaterialRequest request,
            Authentication authentication) {
        MaterialDetailResponse material = materialService.update(authentication.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success(material, "Material updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        materialService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<StorageService.StorageInfo>> getStorage(Authentication authentication) {
        StorageService.StorageInfo info = storageService.getStorageInfo(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(info, "Storage info retrieved"));
    }
}
