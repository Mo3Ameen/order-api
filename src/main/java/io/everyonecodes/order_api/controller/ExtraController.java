package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.service.ExtraService;
import org.springframework.http.HttpStatus;
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
    public List<Extra> getExtra() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Extra getExtraById(@PathVariable Long id) {
        return service.findExtraByIdOrThrow(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Extra postExtra(@RequestBody Extra extra) {
        return service.createExtra(extra);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Extra putExtra(@RequestBody Extra extra, @PathVariable Long id) {
        return service.updateExtra(extra, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExtra(@PathVariable Long id) {
        service.softDeleteById(id);
    }
}