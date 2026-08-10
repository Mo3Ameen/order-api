package io.everyonecodes.order_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KitchenTicketDto {
    private Long orderId;
    private LocalDateTime createdAt;
    private List<KitchenTicketItemDto> items;
}