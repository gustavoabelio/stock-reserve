package br.com.stockreserve.api;

import br.com.stockreserve.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Respostas {
    private Respostas() {}

    public record UsuarioOutput(long id, String nome, String email, Perfil perfil) {
        public static UsuarioOutput de(Usuario u) { return new UsuarioOutput(u.id(), u.nome(), u.email(), u.perfil()); }
    }

    public record PedidoOutput(long id, long clienteId, Instant dataCriacao,
                               StatusPedido status, List<ItemPedido> itens, BigDecimal total) {
        public static PedidoOutput de(Pedido p) {
            return new PedidoOutput(p.id(), p.clienteId(), p.dataCriacao(), p.status(), p.itens(), p.total());
        }
    }

    public record LoginOutput(String accessToken, String tokenType, Instant expiraEm, UsuarioOutput usuario) {}
}
