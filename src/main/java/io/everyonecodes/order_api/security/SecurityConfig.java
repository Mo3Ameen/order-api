package io.everyonecodes.order_api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth

                        // Extras are fully admin-managed (no public browsing endpoint of their own)
                        .requestMatchers("/api/extras/**").hasRole("ADMIN")

                        // Categories: admin-only listing/mutation, everything else is public
                        .requestMatchers(HttpMethod.GET, "/api/categories/all", "/api/categories/all/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // MenuItems: admin-only listing/mutation, everything else is public
                        .requestMatchers(HttpMethod.GET, "/api/menuItems/all", "/api/menuItems/all/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/menuItems").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/menuItems/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/menuItems/**").hasRole("ADMIN")

                        // Everything else (public catalog browsing + placing/managing orders) is open
                        .anyRequest().permitAll()
                )
                // This is a stateless REST API (it uses HTTP Basic authentication), so CSRF tokens are not applicable.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}
