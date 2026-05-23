package com.company.integrationplatform.transformation;

import com.company.integrationplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transformations")
@RequiredArgsConstructor
@Tag(name = "Transformation Engine", description = "APIs for managing data transformation rules and field mappings")
public class TransformationController {

    private final TransformationService transformationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Create a new transformation rule")
    public ResponseEntity<ApiResponse<TransformationDto.Response>> create(
            @Valid @RequestBody TransformationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transformation rule created",
                        transformationService.createRule(request)));
    }

    @GetMapping
    @Operation(summary = "Get all transformation rules")
    public ResponseEntity<ApiResponse<List<TransformationDto.Response>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(transformationService.getAllRules()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transformation rule by ID")
    public ResponseEntity<ApiResponse<TransformationDto.Response>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(transformationService.getRuleById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Update a transformation rule")
    public ResponseEntity<ApiResponse<TransformationDto.Response>> update(
            @PathVariable UUID id,
            @RequestBody TransformationDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rule updated",
                transformationService.updateRule(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a transformation rule")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        transformationService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted", null));
    }
}
