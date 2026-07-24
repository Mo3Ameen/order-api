package io.everyonecodes.order_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ordered_items")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Integer quantity;
    @Column(precision = 5, scale = 2)
    private BigDecimal priceAtPurchase;
    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "order_id")
    private Order order;
    @OneToMany(mappedBy = "orderedItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SelectedExtra> selectedExtras = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderedItem other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}