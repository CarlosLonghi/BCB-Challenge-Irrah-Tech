package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.request.ClientRequest;
import com.carloslonghi.bcb.controller.response.ClientBalanceResponse;
import com.carloslonghi.bcb.controller.response.ClientResponse;
import com.carloslonghi.bcb.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Client <-> DTOs. O toEntity ignora passwordHash: a senha vira hash no ClientService.create.
 */
@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    Client toEntity(ClientRequest request);

    ClientResponse toResponse(Client entity);

    ClientBalanceResponse toBalanceResponse(Client entity);
}
