package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.response.LedgerEntryResponse;
import com.carloslonghi.bcb.entity.LedgerEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LedgerEntryMapper {

    @Mapping(target = "messageId", source = "message.id")
    LedgerEntryResponse toResponse(LedgerEntry entity);
}
