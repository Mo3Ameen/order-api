package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.ExtraRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ExtraService {

    private final ExtraRepository repository;

    public ExtraService(ExtraRepository repository) {
        this.repository = repository;
    }

    public List<Extra> findByMenuItemsContainingAndIsActive(MenuItem menuItem) {
        return repository.findByMenuItemsContainingAndIsActive(menuItem, true);
    }

    public List<Extra> findAll() {
        return repository.findAll();
    }

    public Extra updateExtra(Extra extra, Long id) {
        Extra existingExtra = findExtraByIdOrThrow(id);
        existingExtra.setName(extra.getName());
        existingExtra.setIsActive(extra.getIsActive());
        existingExtra.setPrice(extra.getPrice());
        return repository.save(existingExtra);
    }

    public Extra createExtra(Extra extra) {
        extra.setId(null);
        extra.setIsActive(extra.getIsActive() != null ? extra.getIsActive() : true);
        return repository.save(extra);
    }

    public void softDeleteById(Long id) {
        Extra extra = findExtraByIdOrThrow(id);
        extra.setIsActive(false);
        repository.save(extra);
    }

    public Extra findExtraByIdOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Extra " + id + " not found"));
    }

    public List<Extra> findAllById(Set<Long> extraIds) {
        return repository.findAllById(extraIds);
    }
}