package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.repository.ExtraRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ExtraService {

    private final ExtraRepository repository;

    public ExtraService(ExtraRepository repository) {
        this.repository = repository;
    }

    public List<Extra> findAll() {
        return repository.findAll();
    }

    public Optional<Extra> findById(Long id) {
        return repository.findById(id);
    }

    public Extra save(Extra extra) {
        return repository.save(extra);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Extra> findAllById(Set<Long> extraIds) {
        return repository.findAllById(extraIds);
    }
}