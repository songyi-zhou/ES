package org.zhou.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            log.info("Processing request to {}", request.getRequestURI());
            String jwt = getJwtFromRequest(request);
            log.info("Request URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
            log.info("JWT Token present: {}", StringUtils.hasText(jwt));

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String userId = tokenProvider.getUserIdFromJWT(jwt);
                log.info("JWT token valid for user ID: {}", userId);

                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                log.info("User loaded: {}, Roles: {}", userDetails.getUsername(), 
                         userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Successfully set authentication in SecurityContext");
            } else if (StringUtils.hasText(jwt)) {
                log.warn("Invalid JWT token");
            } else {
                log.info("No JWT token found in request");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
