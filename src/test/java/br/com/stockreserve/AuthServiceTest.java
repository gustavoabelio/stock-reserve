package br.com.stockreserve;

import br.com.stockreserve.api.Requisicoes.LoginInput;
import br.com.stockreserve.domain.*;
import br.com.stockreserve.infra.MemoriaDadosLoja;
import br.com.stockreserve.security.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AuthServiceTest {
    @Test void tokenExpiraAposOitoHoras() {
        var dados = new MemoriaDadosLoja();
        var encoder = new BCryptPasswordEncoder(4);
        dados.salvar(new Usuario(1, "Admin", "admin@teste.com", encoder.encode("SenhaTeste12345"), Perfil.ADMIN));
        var clock = mock(Clock.class);
        var inicio = Instant.parse("2026-01-01T00:00:00Z");
        when(clock.instant()).thenReturn(inicio);
        var auth = new AuthService(dados, encoder, clock, new br.com.stockreserve.config.AppConfig.AuthProperties(Duration.ofHours(8)));
        var login = auth.login(new LoginInput("admin@teste.com", "SenhaTeste12345"));
        assertThat(auth.autenticar(login.accessToken())).isPresent();
        when(clock.instant()).thenReturn(inicio.plus(Duration.ofHours(8)));
        assertThat(auth.autenticar(login.accessToken())).isEmpty();
    }
}
