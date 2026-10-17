package br.com.stockreserve.domain;

public record ItemPedido(long produtoId, int quantidade, java.math.BigDecimal precoUnitario) {}
