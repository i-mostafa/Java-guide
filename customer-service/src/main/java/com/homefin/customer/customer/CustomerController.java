package com.homefin.customer.customer;

import com.homefin.common.web.PageResponse;
import com.homefin.customer.customer.dto.CustomerResponse;
import com.homefin.customer.customer.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper mapper;

    @Operation(summary = "Get my customer profile")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse me(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(customerService.getByUserId(userId(jwt)));
    }

    @Operation(summary = "Update my profile")
    @PutMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return mapper.toResponse(customerService.updateProfile(userId(jwt), request));
    }

    @Operation(summary = "Run identity verification (KYC) with the 3rd-party provider")
    @PostMapping("/me/kyc")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse verifyKyc(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(customerService.verifyKyc(userId(jwt)));
    }

    @Operation(summary = "[Admin] Search customers, e.g. ?kycStatus=PENDING&page=0&size=20&sort=createdAt,desc")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CustomerResponse> search(
            @RequestParam(required = false) KycStatus kycStatus,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(customerService.search(kycStatus, pageable), mapper::toResponse);
    }

    @Operation(summary = "[Admin] Get a customer by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse byId(@PathVariable UUID id) {
        return mapper.toResponse(customerService.getById(id));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
