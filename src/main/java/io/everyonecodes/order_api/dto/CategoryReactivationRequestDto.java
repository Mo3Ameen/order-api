package io.everyonecodes.order_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryReactivationRequestDto {
    private Set<Long> menuItemIds;
}