package br.com.stockreserve.service;

import br.com.stockreserve.api.Requisicoes.UsuarioInput;
import br.com.stockreserve.domain.Usuario;
import br.com.stockreserve.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import static br.com.stockreserve.service.RegraNegocioException.conflito;

@Service
public class UsuarioService {
    private final DadosLoja dados;
    private final UnidadeTrabalho transacao;
    private final PasswordEncoder encoder;

    public UsuarioService(DadosLoja dados, UnidadeTrabalho transacao, PasswordEncoder encoder) {
        this.dados = dados;
        this.transacao = transacao;
        this.encoder = encoder;
    }

    public Usuario criar(UsuarioInput input) {
        if (input.senha().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new RegraNegocioException(400, "A senha deve ter no máximo 72 bytes em UTF-8.");
        String email = input.email().strip().toLowerCase(Locale.ROOT);
        String hash = encoder.encode(input.senha());
        return transacao.executar(() -> {
            if (dados.usuarioPorEmail(email).isPresent()) throw conflito("Já existe um usuário com este e-mail.");
            var usuario = new Usuario(dados.proximoId(DadosLoja.Registro.USUARIO), input.nome().strip(), email, hash, input.perfil());
            dados.salvar(usuario);
            return usuario;
        });
    }

    public Usuario buscar(long id) { return dados.usuario(id).orElseThrow(() -> RegraNegocioException.naoEncontrado("Usuário")); }

    public List<Usuario> listar() { return dados.usuarios(); }
}
