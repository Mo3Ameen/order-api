package io.everyonecodes.order_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryItemDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Boolean isActive;
    private Boolean wasActiveBeforeTurnOff;
}