package br.com.stockreserve.infra;

import br.com.stockreserve.domain.*;
import br.com.stockreserve.repository.DadosLoja;
import br.com.stockreserve.repository.UnidadeTrabalho;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.function.Supplier;

@Repository
public class MemoriaDadosLoja implements DadosLoja, UnidadeTrabalho {
    private Map<Long, Cliente> clientes = new TreeMap<>();
    private Map<Long, Produto> produtos = new TreeMap<>();
    private Map<Long, Pedido> pedidos = new TreeMap<>();
    private Map<Long, MovimentacaoEstoque> movimentacoes = new TreeMap<>();
    private Map<Long, Usuario> usuarios = new TreeMap<>();
    private final Map<Registro, Long> sequencias = new EnumMap<>(Registro.class);

    @Override
    public synchronized <T> T consultar(Supplier<T> consulta) {
        return consulta.get();
    }

    @Override
    public synchronized <T> T executar(Supplier<T> operacao) {
        var clientesAntes = new TreeMap<>(clientes);
        var produtosAntes = new TreeMap<>(produtos);
        var pedidosAntes = new TreeMap<>(pedidos);
        var movimentacoesAntes = new TreeMap<>(movimentacoes);
        var usuariosAntes = new TreeMap<>(usuarios);
        try {
            return operacao.get();
        } catch (RuntimeException e) {
            clientes = clientesAntes;
            produtos = produtosAntes;
            pedidos = pedidosAntes;
            movimentacoes = movimentacoesAntes;
            usuarios = usuariosAntes;
            throw e;
        }
    }

    @Override public synchronized long proximoId(Registro registro) { return sequencias.merge(registro, 1L, Long::sum); }
    @Override public synchronized void salvar(Cliente c) { clientes.put(c.id(), c); }
    @Override public synchronized void salvar(Produto p) { produtos.put(p.id(), p); }
    @Override public synchronized void salvar(Pedido p) { pedidos.put(p.id(), p); }
    @Override public synchronized void salvar(MovimentacaoEstoque m) { movimentacoes.put(m.id(), m); }
    @Override public synchronized void salvar(Usuario u) { usuarios.put(u.id(), u); }
    @Override public synchronized Optional<Cliente> cliente(long id) { return Optional.ofNullable(clientes.get(id)); }
    @Override public synchronized Optional<Produto> produto(long id) { return Optional.ofNullable(produtos.get(id)); }
    @Override public synchronized Optional<Pedido> pedido(long id) { return Optional.ofNullable(pedidos.get(id)); }
    @Override public synchronized Optional<Usuario> usuario(long id) { return Optional.ofNullable(usuarios.get(id)); }
    @Override public synchronized Optional<Usuario> usuarioPorEmail(String email) {
        return usuarios.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }
    @Override public synchronized List<Cliente> clientes() { return List.copyOf(clientes.values()); }
    @Override public synchronized List<Produto> produtos() { return List.copyOf(produtos.values()); }
    @Override public synchronized List<Pedido> pedidos() { return List.copyOf(pedidos.values()); }
    @Override public synchronized List<MovimentacaoEstoque> movimentacoes() { return List.copyOf(movimentacoes.values()); }
    @Override public synchronized List<Usuario> usuarios() { return List.copyOf(usuarios.values()); }
}
