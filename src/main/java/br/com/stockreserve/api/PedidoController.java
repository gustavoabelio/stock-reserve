package br.com.stockreserve.api;

import br.com.stockreserve.api.Requisicoes.PedidoInput;
import br.com.stockreserve.api.Respostas.PedidoOutput;
import br.com.stockreserve.security.UsuarioAutenticado;
import br.com.stockreserve.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final LojaService loja;
    public PedidoController(LojaService loja) { this.loja = loja; }
    @GetMapping public List<PedidoOutput> listar() { return loja.pedidos().stream().map(PedidoOutput::de).toList(); }
    @GetMapping("/{id}") public PedidoOutput buscar(@PathVariable long id) { return PedidoOutput.de(loja.pedido(id)); }
    @PostMapping
    public ResponseEntity<PedidoOutput> criar(@Valid @RequestBody PedidoInput input) {
        var pedido = loja.criarPedido(input);
        return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.id())).body(PedidoOutput.de(pedido));
    }
    @PostMapping("/{id}/confirmar")
    public PedidoOutput confirmar(@PathVariable long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return PedidoOutput.de(loja.confirmarPedido(id, usuario.id()));
    }
    @PostMapping("/{id}/cancelar")
    public PedidoOutput cancelar(@PathVariable long id) { return PedidoOutput.de(loja.cancelarPedido(id)); }
}
