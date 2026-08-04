package io.everyonecodes.order_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddItemRequestDto {
    @NotNull(message = "Item ID cannot be null")
    private Long menuItemId;
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    private Set<Long> extraIds;
}
