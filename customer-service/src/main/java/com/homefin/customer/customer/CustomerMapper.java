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
 *
 * <p>Converts the {@link Customer} entity into the {@link CustomerResponse} DTO. In TS you'd hand-write a
 * {@code toResponse(c: Customer): CustomerResponse} function (or use class-transformer); MapStruct writes that
 * plain getter/constructor code for you, with no reflection at runtime.
 *
 * <ul>
 *   <li>{@code @Mapper(componentModel = SPRING)} (compile time): tells the MapStruct annotation processor to generate
 *       {@code CustomerMapperImpl} and annotate it with {@code @Component}, so at runtime Spring registers it as a
 *       bean and you inject {@code CustomerMapper} like any other dependency.</li>
 *   <li>Fields with the same name (id, email, firstName...) are mapped automatically.</li>
 * </ul>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    // @Mapping (compile time): target field nationalIdMasked gets its value from source field nationalId,
    // passed through the helper method whose @Named qualifier is "mask".
    @Mapping(target = "nationalIdMasked", source = "nationalId", qualifiedByName = "mask")
    CustomerResponse toResponse(Customer customer);

    // @Named (compile time): gives this helper a name so @Mapping can reference it via qualifiedByName.
    // A "default" method is an interface method WITH a body (the generated class inherits it as-is).
    @Named("mask")
    default String mask(String value) {
        // "||" short-circuits like JS, so value.length() is never called on null.
        if (value == null || value.length() < 4) {
            return value;
        }
        // Keep the last 4 characters: "784199012345671" -> "***********5671".
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
