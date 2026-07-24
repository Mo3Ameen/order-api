package io.everyonecodes.order_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "selected_extras")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SelectedExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(precision = 5, scale = 2)
    private BigDecimal priceAtPurchase;
    @ManyToOne
    @JoinColumn(name = "extra_id")
    private Extra extra;
    @ManyToOne
    @JoinColumn(name = "ordered_item_id")
    @JsonIgnore
    private OrderedItem orderedItem;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectedExtra other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}