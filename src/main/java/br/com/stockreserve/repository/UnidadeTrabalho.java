package br.com.stockreserve.repository;

import java.util.function.Supplier;

public interface UnidadeTrabalho {
    <T> T consultar(Supplier<T> consulta);
    <T> T executar(Supplier<T> operacao);
}
