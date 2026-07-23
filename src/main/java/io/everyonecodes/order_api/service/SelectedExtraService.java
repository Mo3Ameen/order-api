package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.SelectedExtra;
import io.everyonecodes.order_api.repository.SelectedExtraRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SelectedExtraService {

    private final SelectedExtraRepository repository;

    public SelectedExtraService(SelectedExtraRepository repository) {
        this.repository = repository;
    }

    public List<SelectedExtra> findAll() {
        return repository.findAll();
    }

    public Optional<SelectedExtra> findById(Long id) {
        return repository.findById(id);
    }

    public SelectedExtra save(SelectedExtra selectedExtra) {
        return repository.save(selectedExtra);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}