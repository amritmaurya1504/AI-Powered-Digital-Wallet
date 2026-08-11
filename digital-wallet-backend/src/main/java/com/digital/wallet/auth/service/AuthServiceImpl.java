package com.digital.wallet.auth.service;

import com.digital.wallet.auth.dto.LoginRequest;
import com.digital.wallet.auth.dto.LoginResponse;
import com.digital.wallet.auth.dto.SignupResponse;
import com.digital.wallet.common.dto.CreateUserRequest;
import com.digital.wallet.user.domain.User;
import com.digital.wallet.user.dto.UserResponse;
import com.digital.wallet.user.service.UserService;
import com.digital.wallet.wallet.dto.WalletResponse;
import com.digital.wallet.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserService userService;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public SignupResponse signup(CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        WalletResponse wallet = walletService.createWallet(user.id());
        return new SignupResponse(user, wallet);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("USER_LOGIN_STARTED email={}", request.email());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authentication.getPrincipal();
        log.info("USER_AUTHENTICATED userId={} email={}", user.getId(), user.getEmailId());

        String token = jwtService.generateToken(user);

        log.info("USER_LOGIN_SUCCESS userId={} email={}",user.getId(),user.getEmailId());
        return new LoginResponse(user.getId(), token);
    }
}
