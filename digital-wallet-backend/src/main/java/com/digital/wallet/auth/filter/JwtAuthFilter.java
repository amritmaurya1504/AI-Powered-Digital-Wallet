package com.digital.wallet.auth.filter;

import com.digital.wallet.auth.service.JwtService;
import com.digital.wallet.user.domain.User;
import com.digital.wallet.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final HandlerExceptionResolver exceptionResolver;
    private final UserService userService;

    public JwtAuthFilter(JwtService jwtService,
                         @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
                         UserService userService) {
        this.jwtService = jwtService;
        this.exceptionResolver = exceptionResolver;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            final String tokenFromReqHeader = request.getHeader("Authorization");

            if(tokenFromReqHeader == null || !tokenFromReqHeader.startsWith("Bearer")){
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("JWT_AUTHENTICATION_STARTED uri={}", request.getRequestURI());
            String token = tokenFromReqHeader.split("Bearer ")[1];
            String userName = jwtService.getUserNameFromToken(token);

            if(userName != null && SecurityContextHolder.getContext().getAuthentication() == null){
                User user = userService.findUserByEmail(userName);
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                log.debug("JWT_AUTHENTICATION_SUCCESS uri={}", request.getRequestURI());
            }

            filterChain.doFilter(request, response);
        }catch (Exception ex){
            log.warn("JWT_AUTHENTICATION_FAILED reason={} uri={}", ex.getClass().getSimpleName(), request.getRequestURI());
            exceptionResolver.resolveException(request, response, null, ex);
        }

    }
}
