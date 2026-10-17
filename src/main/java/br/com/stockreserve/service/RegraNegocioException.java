package br.com.stockreserve.service;

public class RegraNegocioException extends RuntimeException {
    private final int status;

    public RegraNegocioException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int status() { return status; }
    public static RegraNegocioException naoEncontrado(String entidade) {
        return new RegraNegocioException(404, entidade + " não encontrado(a).");
    }
    public static RegraNegocioException conflito(String mensagem) {
        return new RegraNegocioException(409, mensagem);
    }
}
