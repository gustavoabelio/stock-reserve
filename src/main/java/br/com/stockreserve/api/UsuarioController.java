package br.com.stockreserve.api;

import br.com.stockreserve.api.Requisicoes.UsuarioInput;
import br.com.stockreserve.api.Respostas.UsuarioOutput;
import br.com.stockreserve.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;
    public UsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping public List<UsuarioOutput> listar() { return usuarios.listar().stream().map(UsuarioOutput::de).toList(); }
    @GetMapping("/{id}") public UsuarioOutput buscar(@PathVariable long id) { return UsuarioOutput.de(usuarios.buscar(id)); }
    @PostMapping
    public ResponseEntity<UsuarioOutput> criar(@Valid @RequestBody UsuarioInput input) {
        var usuario = usuarios.criar(input);
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id())).body(UsuarioOutput.de(usuario));
    }
}
