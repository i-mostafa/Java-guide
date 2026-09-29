package com.homefin.application.finance;

import com.homefin.application.finance.dto.ApplicationResponse;
import com.homefin.application.finance.dto.CreateApplicationRequest;
import com.homefin.application.finance.dto.UpdateStatusRequest;
import com.homefin.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class FinanceApplicationController {

    private final FinanceApplicationService service;
    private final ApplicationMapper mapper;

    @Operation(summary = "Submit a property finance application")
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApplicationResponse> submit(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody CreateApplicationRequest request) {
        FinanceApplication app = service.submit(UUID.fromString(jwt.getSubject()), request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(app.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(app)); // 201 + Location header
    }

    @Operation(summary = "List my applications")
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<ApplicationResponse> mine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listForCustomer(UUID.fromString(jwt.getSubject()), pageable),
                mapper::toResponse);
    }

    /** @PostAuthorize: load first, then check ownership on the returned object (authentication.name = JWT sub). */
    @Operation(summary = "Get one application (owner or admin)")
    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or returnObject.customerUserId().toString() == authentication.name")
    public ApplicationResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.get(id));
    }

    @Operation(summary = "[Admin] List applications by status")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ApplicationResponse> all(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(service.listByStatus(status, pageable), mapper::toResponse);
    }

    @Operation(summary = "[Admin] Move an application to a new status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApplicationResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return mapper.toResponse(service.changeStatus(id, request));
    }
}
