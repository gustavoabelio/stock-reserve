package br.com.stockreserve.domain;

public record Produto(long id, String sku, String nome, String descricao, java.math.BigDecimal precoAtual) {}
