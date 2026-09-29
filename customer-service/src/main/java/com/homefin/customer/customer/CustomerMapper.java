package com.homefin.customer.customer;

import com.homefin.customer.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * MapStruct generates the implementation (CustomerMapperImpl) at COMPILE time -> fast, type-safe,
 * and the build fails if a target field is left unmapped by mistake.
 * Look at target/generated-sources/annotations to see the generated code.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    @Mapping(target = "nationalIdMasked", source = "nationalId", qualifiedByName = "mask")
    CustomerResponse toResponse(Customer customer);

    @Named("mask")
    default String mask(String value) {
        if (value == null || value.length() < 4) {
            return value;
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
