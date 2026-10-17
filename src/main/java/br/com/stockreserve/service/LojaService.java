package br.com.stockreserve.service;

import br.com.stockreserve.api.Requisicoes.*;
import br.com.stockreserve.domain.*;
import br.com.stockreserve.repository.*;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.*;
import static br.com.stockreserve.repository.DadosLoja.Registro.*;
import static br.com.stockreserve.service.RegraNegocioException.*;

@Service
public class LojaService {
    private final DadosLoja dados;
    private final UnidadeTrabalho transacao;
    private final Clock clock;

    public LojaService(DadosLoja dados, UnidadeTrabalho transacao, Clock clock) {
        this.dados = dados;
        this.transacao = transacao;
        this.clock = clock;
    }

    public Cliente criarCliente(ClienteInput input) {
        return transacao.executar(() -> {
            String email = input.email().strip().toLowerCase(Locale.ROOT);
            if (dados.clientes().stream().anyMatch(c -> c.email().equals(email)))
                throw conflito("Já existe um cliente com este e-mail.");
            var cliente = new Cliente(dados.proximoId(CLIENTE), input.nome().strip(), email);
            dados.salvar(cliente);
            return cliente;
        });
    }

    public List<Cliente> clientes() { return dados.clientes(); }
    public Cliente cliente(long id) { return dados.cliente(id).orElseThrow(() -> naoEncontrado("Cliente")); }
    public List<Produto> produtos() { return dados.produtos(); }
    public Produto produto(long id) { return dados.produto(id).orElseThrow(() -> naoEncontrado("Produto")); }
    public List<Pedido> pedidos() { return dados.pedidos(); }
    public Pedido pedido(long id) { return dados.pedido(id).orElseThrow(() -> naoEncontrado("Pedido")); }

    public Produto criarProduto(ProdutoInput input) {
        return transacao.executar(() -> salvarProduto(dados.proximoId(PRODUTO), input));
    }

    public Produto atualizarProduto(long id, ProdutoInput input) {
        return transacao.executar(() -> { produto(id); return salvarProduto(id, input); });
    }

    private Produto salvarProduto(long id, ProdutoInput input) {
        String sku = input.sku().strip().toUpperCase(Locale.ROOT);
        if (sku.length() > 40)
            throw new RegraNegocioException(400, "O SKU deve ter no máximo 40 caracteres após a conversão para maiúsculas.");
        if (dados.produtos().stream().anyMatch(p -> p.id() != id && p.sku().equals(sku)))
            throw conflito("Já existe um produto com este SKU.");
        var produto = new Produto(id, sku, input.nome().strip(), input.descricao(),
                input.precoAtual().setScale(2, RoundingMode.UNNECESSARY));
        dados.salvar(produto);
        return produto;
    }

    public MovimentacaoEstoque registrarEntrada(EntradaInput input, long usuarioId) {
        return transacao.executar(() -> {
            produto(input.produtoId());
            validarUsuario(usuarioId);
            var movimento = new MovimentacaoEstoque(dados.proximoId(MOVIMENTACAO), input.produtoId(),
                    null, usuarioId, TipoMovimentacao.ENTRADA, input.quantidade(), clock.instant());
            dados.salvar(movimento);
            return movimento;
        });
    }

    public Estoque estoque(long produtoId) {
        return transacao.consultar(() -> { produto(produtoId); return calcularEstoque(produtoId); });
    }

    public List<Estoque> estoques() {
        return transacao.consultar(() -> dados.produtos().stream().map(p -> calcularEstoque(p.id())).toList());
    }

    private Estoque calcularEstoque(long produtoId) {
        long fisico = dados.movimentacoes().stream().filter(m -> m.produtoId() == produtoId)
                .mapToLong(m -> m.tipo() == TipoMovimentacao.ENTRADA ? m.quantidade() : -m.quantidade()).sum();
        long reservado = dados.pedidos().stream().filter(p -> p.status() == StatusPedido.PENDENTE)
                .flatMap(p -> p.itens().stream()).filter(i -> i.produtoId() == produtoId)
                .mapToLong(ItemPedido::quantidade).sum();
        return new Estoque(produtoId, fisico, reservado, fisico - reservado);
    }

    public List<MovimentacaoEstoque> movimentacoes(Long produtoId) {
        return transacao.consultar(() -> {
            if (produtoId != null) produto(produtoId);
            return dados.movimentacoes().stream().filter(m -> produtoId == null || m.produtoId() == produtoId).toList();
        });
    }

    public Pedido criarPedido(PedidoInput input) {
        return transacao.executar(() -> {
            cliente(input.clienteId());
            Set<Long> produtos = new HashSet<>();
            List<ItemPedido> itens = new ArrayList<>();
            for (var item : input.itens()) {
                if (!produtos.add(item.produtoId()))
                    throw new RegraNegocioException(400, "O mesmo produto não pode aparecer em dois itens. Ajuste a quantidade.");
                var produto = produto(item.produtoId());
                if (calcularEstoque(produto.id()).disponivel() < item.quantidade())
                    throw conflito("Estoque insuficiente para o produto " + produto.id() + ".");
                itens.add(new ItemPedido(produto.id(), item.quantidade(), produto.precoAtual()));
            }
            var pedido = new Pedido(dados.proximoId(PEDIDO), input.clienteId(), clock.instant(), StatusPedido.PENDENTE, itens);
            dados.salvar(pedido);
            return pedido;
        });
    }

    public Pedido confirmarPedido(long id, long usuarioId) {
        return transacao.executar(() -> {
            var pedido = pedidoPendente(id);
            validarUsuario(usuarioId);
            for (var item : pedido.itens()) {
                var movimento = new MovimentacaoEstoque(dados.proximoId(MOVIMENTACAO), item.produtoId(), id,
                        usuarioId, TipoMovimentacao.SAIDA, item.quantidade(), clock.instant());
                dados.salvar(movimento);
            }
            var confirmado = pedido.comStatus(StatusPedido.CONFIRMADO);
            dados.salvar(confirmado);
            return confirmado;
        });
    }

    public Pedido cancelarPedido(long id) {
        return transacao.executar(() -> {
            var cancelado = pedidoPendente(id).comStatus(StatusPedido.CANCELADO);
            dados.salvar(cancelado);
            return cancelado;
        });
    }

    private Pedido pedidoPendente(long id) {
        var pedido = pedido(id);
        if (pedido.status() != StatusPedido.PENDENTE)
            throw conflito("Somente pedidos PENDENTES podem ser confirmados ou cancelados.");
        return pedido;
    }

    private void validarUsuario(long id) {
        dados.usuario(id).orElseThrow(() -> naoEncontrado("Usuário"));
    }
}
