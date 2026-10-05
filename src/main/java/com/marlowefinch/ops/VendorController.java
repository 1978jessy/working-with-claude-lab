package com.marlowefinch.ops;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VendorController {

    private final VendorRepository repository;

    public VendorController(VendorRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/vendors")
    public List<Vendor> vendors() {
        return repository.findAll();
    }
}
