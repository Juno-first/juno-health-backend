package com.juno.healthapp.config;


import com.juno.healthapp.dao.AuthAccountDAO;
import com.juno.healthapp.entity.AuthAccount;
import com.juno.healthapp.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthAccountDAO authAccountDAO;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtUtil.extractClaims(token);
        UUID accountId = UUID.fromString(claims.getSubject());
        String accountType = claims.get("accountType", String.class);
        String staffRole = claims.get("staffRole", String.class);

        AuthAccount account = authAccountDAO.findById(accountId).orElse(null);

        if (account != null && Boolean.FALSE.equals(account.getIsLocked())) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // Base role — ROLE_PATIENT or ROLE_STAFF
            authorities.add(new SimpleGrantedAuthority("ROLE_" + accountType));

            // Staff-specific role — ROLE_ADMIN, ROLE_NURSE, ROLE_DOCTOR etc.
            if (staffRole != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + staffRole));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(account, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
