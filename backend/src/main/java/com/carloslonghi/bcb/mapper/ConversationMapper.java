package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.response.ConversationResponse;
import com.carloslonghi.bcb.entity.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(target = "clientId", source = "client.id")
    ConversationResponse toResponse(Conversation entity);
}
