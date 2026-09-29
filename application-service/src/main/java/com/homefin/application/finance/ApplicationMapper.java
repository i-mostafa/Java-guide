package com.homefin.application.finance;

import com.homefin.application.finance.dto.ApplicationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Converts the JPA entity into the API response DTO (never expose entities directly over HTTP).
 *
 * <p>Role: you only declare the method signature. At COMPILE time the MapStruct annotation processor
 * (configured in the parent pom) generates a class {@code ApplicationMapperImpl} in target/generated-sources
 * that copies each property with a matching name: {@code application.getCity()} into the record's
 * {@code city} component, and so on. Because the generated code is plain Java, there is no reflection
 * at runtime and a missing/misspelled field shows up as a compiler warning.
 *
 * <p>TS analogy: a hand-written {@code toResponse(entity): ApplicationResponse} function, or
 * class-transformer's {@code plainToInstance}, except it is code-generated and type-checked.
 */
// @Mapper (compile time, MapStruct): generate an implementation of this interface.
// componentModel = SPRING: the generated class is annotated @Component, so Spring registers it as a bean
// and it can be constructor-injected (see FinanceApplicationController).
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    ApplicationResponse toResponse(FinanceApplication application);
}
