package br.com.stockreserve.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Pedido(long id, long clienteId, Instant dataCriacao, StatusPedido status, List<ItemPedido> itens) {
    public Pedido {
        itens = List.copyOf(itens);
    }

    public BigDecimal total() {
        return itens.stream().map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public Pedido comStatus(StatusPedido novoStatus) {
        return new Pedido(id, clienteId, dataCriacao, novoStatus, itens);
    }
}
