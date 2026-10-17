package br.com.stockreserve.api;

import br.com.stockreserve.domain.Perfil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class Requisicoes {
    private Requisicoes() {}

    public record ClienteInput(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 150) String email) {}

    public record ProdutoInput(
            @NotBlank @Size(max = 40) String sku,
            @NotBlank @Size(max = 100) String nome,
            String descricao,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoAtual) {}

    public record ItemInput(@NotNull @Positive Long produtoId, @Min(1) int quantidade) {}

    public record PedidoInput(
            @NotNull @Positive Long clienteId,
            @NotEmpty List<@NotNull @Valid ItemInput> itens) {}

    public record EntradaInput(@NotNull @Positive Long produtoId, @Min(1) int quantidade) {}

    public record UsuarioInput(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 12, max = 64) String senha,
            @NotNull Perfil perfil) {}

    public record LoginInput(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 64) String senha) {}
}
