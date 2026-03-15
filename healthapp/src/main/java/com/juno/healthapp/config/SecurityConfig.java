package com.juno.healthapp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Facilities
                        .requestMatchers(HttpMethod.GET, "/api/v1/facilities/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/facilities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/facilities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/facilities/**").hasRole("ADMIN")

                        // Departments
                        .requestMatchers(HttpMethod.GET, "/api/v1/departments/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasRole("ADMIN")

                        // Queue - patient routes
                        .requestMatchers(HttpMethod.POST, "/api/v1/queue/checkin").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/queue/status").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/queue/leave").hasRole("PATIENT")

                        // Queue - admin/staff control routes
                        .requestMatchers(HttpMethod.GET, "/api/v1/queue/department/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/queue/call/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/queue/discharge/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/onboarding/**").hasRole("PATIENT")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}