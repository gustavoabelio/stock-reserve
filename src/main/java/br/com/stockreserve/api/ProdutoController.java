package br.com.stockreserve.api;

import br.com.stockreserve.api.Requisicoes.ProdutoInput;
import br.com.stockreserve.domain.Produto;
import br.com.stockreserve.service.LojaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {
    private final LojaService loja;
    public ProdutoController(LojaService loja) { this.loja = loja; }
    @GetMapping public List<Produto> listar() { return loja.produtos(); }
    @GetMapping("/{id}") public Produto buscar(@PathVariable long id) { return loja.produto(id); }
    @PostMapping
    public ResponseEntity<Produto> criar(@Valid @RequestBody ProdutoInput input) {
        var produto = loja.criarProduto(input);
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.id())).body(produto);
    }
    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable long id, @Valid @RequestBody ProdutoInput input) { return loja.atualizarProduto(id, input); }
}
