package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.service.ExtraService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/extras")
public class ExtraController {

    private final ExtraService service;

    public ExtraController(ExtraService service) {
        this.service = service;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Extra> getExtras() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Extra getExtraById(@PathVariable Long id) {
        return getExtra(id);
    }

    @PostMapping
    public ResponseEntity<Extra> createExtra(@RequestBody Extra extra) {
        extra.setId(null);
        Extra saved = service.save(extra);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Extra> updateExtra(@RequestBody Extra extra, @PathVariable Long id) {
        Extra existingExtra = getExtra(id);
        existingExtra.setName(extra.getName());
        existingExtra.setIsActive(extra.getIsActive());
        existingExtra.setPrice(extra.getPrice());
        Extra saved = service.save(existingExtra);
        return ResponseEntity.status(HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExtraById(@PathVariable Long id) {
        getExtra(id);
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private @NonNull Extra getExtra(Long id) {
        return service.findById(id).orElseThrow(() -> new ResourceNotFoundException("Extra " + id + " not found"));
    }
}