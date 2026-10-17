package br.com.stockreserve.domain;

public record MovimentacaoEstoque(long id, long produtoId, Long pedidoId, long usuarioId, TipoMovimentacao tipo, int quantidade, java.time.Instant dataMovimentacao) {}
