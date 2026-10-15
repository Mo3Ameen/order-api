package io.everyonecodes.order_api.dto;

import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExtraRequestDto {
    private String name;
    @Digits(integer = 3, fraction = 2)
    private BigDecimal price;
    private Boolean isActive;
    private Set<Long> menuItemIds;
}
