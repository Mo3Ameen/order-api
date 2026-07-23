package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.OrderedItem;
import io.everyonecodes.order_api.repository.OrderedItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderedItemService {

    private final OrderedItemRepository repository;

    public OrderedItemService(OrderedItemRepository repository) {
        this.repository = repository;
    }

    public List<OrderedItem> findAll() {
        return repository.findAll();
    }

    public Optional<OrderedItem> findById(Long id) {
        return repository.findById(id);
    }

    public OrderedItem save(OrderedItem orderedItem) {
        return repository.save(orderedItem);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}