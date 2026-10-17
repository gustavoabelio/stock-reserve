package br.com.stockreserve.repository;

import br.com.stockreserve.domain.*;
import java.util.List;
import java.util.Optional;

public interface DadosLoja {
    enum Registro { CLIENTE, PRODUTO, PEDIDO, MOVIMENTACAO, USUARIO }
    long proximoId(Registro registro);
    void salvar(Cliente cliente);
    void salvar(Produto produto);
    void salvar(Pedido pedido);
    void salvar(MovimentacaoEstoque movimentacao);
    void salvar(Usuario usuario);
    Optional<Cliente> cliente(long id);
    Optional<Produto> produto(long id);
    Optional<Pedido> pedido(long id);
    Optional<Usuario> usuario(long id);
    Optional<Usuario> usuarioPorEmail(String email);
    List<Cliente> clientes();
    List<Produto> produtos();
    List<Pedido> pedidos();
    List<MovimentacaoEstoque> movimentacoes();
    List<Usuario> usuarios();
}
