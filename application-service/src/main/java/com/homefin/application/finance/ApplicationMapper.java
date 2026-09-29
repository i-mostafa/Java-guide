package com.homefin.application.finance;

import com.homefin.application.finance.dto.ApplicationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    ApplicationResponse toResponse(FinanceApplication application);
}
