package br.com.stockreserve.security;

import br.com.stockreserve.api.Requisicoes.LoginInput;
import br.com.stockreserve.api.Respostas.*;
import br.com.stockreserve.domain.Usuario;
import br.com.stockreserve.config.AppConfig.AuthProperties;
import br.com.stockreserve.repository.DadosLoja;
import br.com.stockreserve.service.RegraNegocioException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private record Sessao(long usuarioId, Instant expiraEm) {}
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final DadosLoja dados;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String hashInexistente;
    private final Duration duracaoToken;

    public AuthService(DadosLoja dados, PasswordEncoder encoder, Clock clock, AuthProperties config) {
        this.dados = dados;
        this.encoder = encoder;
        this.clock = clock;
        this.duracaoToken = config.duracaoToken();
        hashInexistente = encoder.encode(UUID.randomUUID().toString());
    }

    public LoginOutput login(LoginInput input) {
        var usuario = dados.usuarioPorEmail(input.email().strip().toLowerCase(Locale.ROOT));
        if (input.senha().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new RegraNegocioException(401, "E-mail ou senha inválidos.");
        boolean confere = encoder.matches(input.senha(), usuario.map(Usuario::senhaHash).orElse(hashInexistente));
        if (!confere || usuario.isEmpty()) throw new RegraNegocioException(401, "E-mail ou senha inválidos.");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiraEm = clock.instant().plus(duracaoToken);
        sessoes.put(hash(token), new Sessao(usuario.get().id(), expiraEm));
        return new LoginOutput(token, "Bearer", expiraEm, UsuarioOutput.de(usuario.get()));
    }

    public Optional<UsuarioAutenticado> autenticar(String token) {
        if (token == null || token.length() != 43) return Optional.empty();
        String chave = hash(token);
        Sessao sessao = sessoes.get(chave);
        if (sessao == null) return Optional.empty();
        if (!clock.instant().isBefore(sessao.expiraEm())) {
            sessoes.remove(chave);
            return Optional.empty();
        }
        return dados.usuario(sessao.usuarioId()).map(u -> new UsuarioAutenticado(u.id(), u.email(), u.perfil()));
    }

    public void logout(String token) { sessoes.remove(hash(token)); }

    @Scheduled(fixedDelay = 60000)
    public void removerExpiradas() {
        Instant agora = clock.instant();
        sessoes.entrySet().removeIf(e -> !agora.isBefore(e.getValue().expiraEm()));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
