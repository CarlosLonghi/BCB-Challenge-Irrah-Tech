package com.carloslonghi.bcb.mapper;

import com.carloslonghi.bcb.controller.response.MessageResponse;
import com.carloslonghi.bcb.controller.response.SendMessageResponse;
import com.carloslonghi.bcb.entity.Client;
import com.carloslonghi.bcb.entity.Message;
import com.carloslonghi.bcb.entity.enums.ClientPlanType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    MessageResponse toResponse(Message entity);

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "estimatedDelivery", source = "createdAt", qualifiedByName = "estimatedDelivery")
    @Mapping(target = "currentBalance", source = "sender", qualifiedByName = "currentBalance")
    SendMessageResponse toSendResponse(Message entity);

    @Named("estimatedDelivery")
    default Instant estimatedDelivery(Instant createdAt) {
        return createdAt.plusSeconds(30);
    }

    // pre-pago mostra o saldo, pos-pago o limite restante
    @Named("currentBalance")
    default BigDecimal currentBalance(Client sender) {
        return sender.getPlanType() == ClientPlanType.PRE_PAID ? sender.getBalance() : sender.getLimit();
    }
}
