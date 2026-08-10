package io.everyonecodes.order_api;

import io.everyonecodes.order_api.entity.Role;
import io.everyonecodes.order_api.entity.User;
import io.everyonecodes.order_api.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public AdminSeeder(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (repository.findByUserName("admin").isEmpty()) {
            User admin = new User();
            admin.setUserName("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            repository.save(admin);
            System.out.println("Admin user created successfully!");
        } else {
            System.out.println("Admin user already exist.");
        }
    }
}