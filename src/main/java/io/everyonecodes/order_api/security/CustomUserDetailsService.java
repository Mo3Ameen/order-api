package io.everyonecodes.order_api.security;

import io.everyonecodes.order_api.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUserName(username).orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found."));
        return User
                .withUsername(user.getUserName())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}