package br.com.stockreserve.domain;

public record Estoque(long produtoId, long saldoFisico, long reservado, long disponivel) {}
