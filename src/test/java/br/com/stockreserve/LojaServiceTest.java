package br.com.stockreserve;

import br.com.stockreserve.api.Requisicoes.*;
import br.com.stockreserve.domain.*;
import br.com.stockreserve.infra.MemoriaDadosLoja;
import br.com.stockreserve.service.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class LojaServiceTest {
    private MemoriaDadosLoja dados;
    private LojaService loja;
    private Cliente cliente;
    private Produto produto;

    @BeforeEach void preparar() {
        dados = new MemoriaDadosLoja();
        loja = new LojaService(dados, dados, Clock.systemUTC());
        dados.salvar(new Usuario(1, "Administrador", "admin@local.test", "hash", Perfil.ADMIN));
        cliente = loja.criarCliente(new ClienteInput("Cliente", "cliente@teste.com"));
        produto = loja.criarProduto(new ProdutoInput("SKU-1", "Teclado", null, new BigDecimal("100.00")));
        loja.registrarEntrada(new EntradaInput(produto.id(), 5), 1);
    }

    private Pedido reservar(int quantidade) {
        return loja.criarPedido(new PedidoInput(cliente.id(), List.of(new ItemInput(produto.id(), quantidade))));
    }

    @Test void reservaMantemSaldoFisicoEReduzDisponivel() {
        var pedido = reservar(2);
        assertThat(pedido.status()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(pedido.total()).isEqualByComparingTo("200.00");
        assertThat(loja.estoque(produto.id())).isEqualTo(new Estoque(produto.id(), 5, 2, 3));
    }

    @Test void confirmacaoRegistraUmaSaidaEPreservaOutrasReservas() {
        var primeiro = reservar(2);
        reservar(1);
        loja.confirmarPedido(primeiro.id(), 1);
        assertThat(loja.estoque(produto.id())).isEqualTo(new Estoque(produto.id(), 3, 1, 2));
        var saidas = loja.movimentacoes(produto.id()).stream().filter(m -> m.tipo() == TipoMovimentacao.SAIDA).toList();
        assertThat(saidas).hasSize(1);
        assertThat(saidas.get(0).pedidoId()).isEqualTo(primeiro.id());
        assertThat(saidas.get(0).usuarioId()).isEqualTo(1);
        assertThat(saidas.get(0).quantidade()).isEqualTo(2);
        assertThatThrownBy(() -> loja.confirmarPedido(primeiro.id(), 1)).isInstanceOf(RegraNegocioException.class);
        assertThat(loja.movimentacoes(produto.id())).hasSize(2);
    }

    @Test void cancelamentoLiberaReservaSemMovimentacao() {
        var pedido = reservar(3);
        loja.cancelarPedido(pedido.id());
        assertThat(loja.estoque(produto.id())).isEqualTo(new Estoque(produto.id(), 5, 0, 5));
        assertThat(loja.movimentacoes(produto.id())).hasSize(1);
        assertThatThrownBy(() -> loja.confirmarPedido(pedido.id(), 1)).isInstanceOf(RegraNegocioException.class);
    }

    @Test void confirmadoNaoPodeSerCancelado() {
        var pedido = reservar(1);
        loja.confirmarPedido(pedido.id(), 1);
        assertThatThrownBy(() -> loja.cancelarPedido(pedido.id())).isInstanceOf(RegraNegocioException.class);
        assertThat(loja.pedido(pedido.id()).status()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test void precoDoItemNaoMudaQuandoProdutoMuda() {
        var pedido = reservar(2);
        loja.atualizarProduto(produto.id(), new ProdutoInput("SKU-1", "Teclado", null, new BigDecimal("150.00")));
        assertThat(loja.pedido(pedido.id()).total()).isEqualByComparingTo("200.00");
        assertThat(reservar(1).total()).isEqualByComparingTo("150.00");
    }

    @Test void pedidoComItemSemSaldoNaoDeixaReservaParcial() {
        var semSaldo = loja.criarProduto(new ProdutoInput("SKU-2", "Mouse", null, new BigDecimal("30.00")));
        var input = new PedidoInput(cliente.id(), List.of(new ItemInput(produto.id(), 2), new ItemInput(semSaldo.id(), 1)));
        assertThatThrownBy(() -> loja.criarPedido(input)).isInstanceOf(RegraNegocioException.class);
        assertThat(loja.pedidos()).isEmpty();
        assertThat(loja.estoque(produto.id()).reservado()).isZero();
    }

    @Test void naoAceitaMesmoProdutoEmDuasLinhas() {
        var input = new PedidoInput(cliente.id(), List.of(new ItemInput(produto.id(), 1), new ItemInput(produto.id(), 1)));
        assertThatThrownBy(() -> loja.criarPedido(input)).isInstanceOf(RegraNegocioException.class);
        assertThat(loja.pedidos()).isEmpty();
    }

    @Test void doisPedidosDisputamUltimaUnidade() throws Exception {
        reservar(4);
        var executor = Executors.newFixedThreadPool(2);
        var largada = new CountDownLatch(1);
        Callable<Boolean> tentativa = () -> {
            largada.await();
            try { reservar(1); return true; }
            catch (RegraNegocioException e) { assertThat(e.status()).isEqualTo(409); return false; }
        };
        try {
            var a = executor.submit(tentativa);
            var b = executor.submit(tentativa);
            largada.countDown();
            assertThat(List.of(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(loja.estoque(produto.id())).isEqualTo(new Estoque(produto.id(), 5, 5, 0));
        } finally { executor.shutdownNow(); }
    }

    @Test void exclusaoMutuaTambemRestauraDadosQuandoOperacaoFalha() {
        assertThatThrownBy(() -> dados.executar(() -> {
            dados.salvar(new Cliente(999, "Não deve permanecer", "rollback@teste.com"));
            throw new IllegalStateException("falha simulada");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(dados.cliente(999)).isEmpty();
    }

    @Test void unicidadeIgnoraMaiusculasEmEmailESku() {
        assertThatThrownBy(() -> loja.criarCliente(new ClienteInput("Outro", "CLIENTE@TESTE.COM")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> loja.criarProduto(new ProdutoInput("sku-1", "Outro", null, new BigDecimal("50.00"))))
                .isInstanceOf(RegraNegocioException.class);
    }
}
