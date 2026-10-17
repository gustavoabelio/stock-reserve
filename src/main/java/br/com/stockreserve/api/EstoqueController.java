package br.com.stockreserve.api;

import br.com.stockreserve.api.Requisicoes.EntradaInput;
import br.com.stockreserve.domain.*;
import br.com.stockreserve.security.UsuarioAutenticado;
import br.com.stockreserve.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {
    private final LojaService loja;
    public EstoqueController(LojaService loja) { this.loja = loja; }
    @GetMapping public List<Estoque> listar() { return loja.estoques(); }
    @GetMapping("/{produtoId}") public Estoque consultar(@PathVariable long produtoId) { return loja.estoque(produtoId); }
    @GetMapping("/movimentacoes")
    public List<MovimentacaoEstoque> movimentacoes(@RequestParam(required = false) Long produtoId) {
        return loja.movimentacoes(produtoId);
    }
    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoEstoque entrada(@Valid @RequestBody EntradaInput input, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return loja.registrarEntrada(input, usuario.id());
    }
}
