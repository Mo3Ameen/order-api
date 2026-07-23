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
@Table(name = "menu_items")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(precision = 5, scale = 2)
    private BigDecimal price;
    private String imageUrl;
    private Boolean isActive;
    @ManyToMany(mappedBy = "menuItems")
    private Set<Extra> extras = new HashSet<>();
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}