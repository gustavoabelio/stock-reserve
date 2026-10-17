package br.com.stockreserve.config;

import br.com.stockreserve.api.Requisicoes.UsuarioInput;
import br.com.stockreserve.domain.Perfil;
import br.com.stockreserve.service.UsuarioService;
import jakarta.validation.constraints.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({AppConfig.AdminProperties.class, AppConfig.AuthProperties.class})
public class AppConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    ApplicationRunner administradorInicial(UsuarioService usuarios, AdminProperties config) {
        return args -> usuarios.criar(new UsuarioInput(config.nome(), config.email(), config.senha(), Perfil.ADMIN));
    }

    @ConfigurationProperties("app.auth")
    public record AuthProperties(Duration duracaoToken) {
        public AuthProperties {
            if (duracaoToken == null || duracaoToken.isZero() || duracaoToken.isNegative())
                throw new IllegalArgumentException("A duração do token deve ser positiva.");
        }
    }

    @Validated
    @ConfigurationProperties("app.admin")
    public record AdminProperties(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 12, max = 64, message = "Defina APP_ADMIN_PASSWORD com 12 a 64 caracteres") String senha) {
        @Override public String toString() { return "AdminProperties[senha omitida]"; }
    }
}
