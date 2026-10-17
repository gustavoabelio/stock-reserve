package br.com.stockreserve.api;

import br.com.stockreserve.api.Requisicoes.LoginInput;
import br.com.stockreserve.api.Respostas.LoginOutput;
import br.com.stockreserve.security.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginOutput login(@Valid @RequestBody LoginInput input) { return auth.login(input); }

    @GetMapping("/me")
    public UsuarioAutenticado me(@AuthenticationPrincipal UsuarioAutenticado usuario) { return usuario; }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorization) { auth.logout(authorization.substring(7)); }
}
