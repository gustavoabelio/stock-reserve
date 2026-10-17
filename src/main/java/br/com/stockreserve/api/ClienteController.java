package br.com.stockreserve.api;

import br.com.stockreserve.api.Requisicoes.ClienteInput;
import br.com.stockreserve.domain.Cliente;
import br.com.stockreserve.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final LojaService loja;
    public ClienteController(LojaService loja) { this.loja = loja; }
    @GetMapping public List<Cliente> listar() { return loja.clientes(); }
    @GetMapping("/{id}") public Cliente buscar(@PathVariable long id) { return loja.cliente(id); }
    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody ClienteInput input) {
        var cliente = loja.criarCliente(input);
        return ResponseEntity.created(URI.create("/api/clientes/" + cliente.id())).body(cliente);
    }
}
