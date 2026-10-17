package br.com.stockreserve;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.admin.senha=SenhaApenasDeTeste123")
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    private String admin;

    @BeforeEach void loginAdmin() throws Exception { admin = login("admin@local.test", "SenhaApenasDeTeste123"); }

    private String login(String email, String senha) throws Exception {
        var result = mvc.perform(json(post("/api/auth/login"), Map.of("email", email, "senha", senha)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.senhaHash").doesNotExist()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
    }
    private JsonNode criar(String path, Object body) throws Exception {
        return mapper.readTree(mvc.perform(json(post(path), body).header("Authorization", "Bearer " + admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private long cliente() throws Exception {
        return criar("/api/clientes", Map.of("nome", "Cliente", "email", UUID.randomUUID() + "@teste.com")).get("id").asLong();
    }
    private long produto() throws Exception {
        return criar("/api/produtos", Map.of("sku", UUID.randomUUID().toString(), "nome", "Produto", "precoAtual", 25.50)).get("id").asLong();
    }
    private Map<String, Object> pedido(long clienteId, long produtoId, int quantidade) {
        return Map.of("clienteId", clienteId, "itens", List.of(Map.of("produtoId", produtoId, "quantidade", quantidade)));
    }

    @Test void exigeTokenERecusaCredenciaisErradas() throws Exception {
        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/produtos").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), Map.of("email", "admin@local.test", "senha", "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test void logoutInvalidaToken() throws Exception {
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + admin)).andExpect(status().isUnauthorized());
    }

    @Test void operadorNaoPodeExecutarOperacoesAdministrativas() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        criar("/api/usuarios", Map.of("nome", "Operador", "email", email, "senha", "SenhaOperador123", "perfil", "OPERADOR"));
        String operador = login(email, "SenhaOperador123");
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + operador)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/produtos"), Map.of("sku", "NEGADO", "nome", "Produto", "precoAtual", 10))
                .header("Authorization", "Bearer " + operador)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/estoque/entradas"), Map.of("produtoId", 1, "quantidade", 1))
                .header("Authorization", "Bearer " + operador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/produtos").header("Authorization", "Bearer " + operador)).andExpect(status().isOk());
        mvc.perform(json(post("/api/clientes"), Map.of("nome", "Cliente do operador", "email", UUID.randomUUID() + "@teste.com"))
                .header("Authorization", "Bearer " + operador)).andExpect(status().isCreated());
    }

    @Test void cicloCompletoPelaApi() throws Exception {
        long cliente = cliente(), produto = produto();
        criar("/api/estoque/entradas", Map.of("produtoId", produto, "quantidade", 5));
        var pedido = criar("/api/pedidos", pedido(cliente, produto, 2));
        long id = pedido.get("id").asLong();
        assertThat(pedido.get("total").decimalValue()).isEqualByComparingTo("51.00");
        mvc.perform(get("/api/estoque/" + produto).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.saldoFisico").value(5)).andExpect(jsonPath("$.reservado").value(2))
                .andExpect(jsonPath("$.disponivel").value(3));
        mvc.perform(post("/api/pedidos/" + id + "/confirmar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMADO"));
        mvc.perform(post("/api/pedidos/" + id + "/confirmar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/estoque/" + produto).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.saldoFisico").value(3)).andExpect(jsonPath("$.reservado").value(0))
                .andExpect(jsonPath("$.disponivel").value(3));
    }

    @Test void cancelamentoPelaApiLiberaReserva() throws Exception {
        long cliente = cliente(), produto = produto();
        criar("/api/estoque/entradas", Map.of("produtoId", produto, "quantidade", 1));
        long id = criar("/api/pedidos", pedido(cliente, produto, 1)).get("id").asLong();
        mvc.perform(post("/api/pedidos/" + id + "/cancelar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(get("/api/estoque/" + produto).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.disponivel").value(1));
    }

    @Test void validaCamposERecusaDadosQueClienteNaoPodeDefinir() throws Exception {
        long cliente = cliente(), produto = produto();
        mvc.perform(json(post("/api/pedidos"), pedido(cliente, produto, 0)).header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros").isArray());
        mvc.perform(json(post("/api/pedidos"), Map.of("clienteId", cliente, "itens", List.of()))
                .header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/produtos"), Map.of("sku", "NEGATIVO", "nome", "Produto", "precoAtual", -1))
                .header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/pedidos"), Map.of("clienteId", cliente, "status", "CONFIRMADO", "itens", List.of()))
                .header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
    }

    @Test void quantidadeFracionariaEPerfilNumericoSaoInvalidos() throws Exception {
        mvc.perform(json(post("/api/estoque/entradas"), Map.of("produtoId", 1, "quantidade", 1.5))
                .header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/usuarios"), Map.of("nome", "Inválido", "email", "invalido@teste.com", "senha", "SenhaApenasDeTeste123", "perfil", 0))
                .header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
    }

    @Test void estoqueInsuficienteRetorna409EReferenciaInexistente404() throws Exception {
        long cliente = cliente(), produto = produto();
        mvc.perform(json(post("/api/pedidos"), pedido(cliente, produto, 1)).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/pedidos"), pedido(999999, produto, 1)).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test void skuQueCresceNaConversaoParaMaiusculasERecusado() throws Exception {
        var body = Map.of("sku", "ß".repeat(40), "nome", "Produto", "precoAtual", 10);
        mvc.perform(json(post("/api/produtos"), body).header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("40 caracteres")));
        long id = produto();
        mvc.perform(json(put("/api/produtos/" + id), body).header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/produtos/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value(org.hamcrest.Matchers.not("SS".repeat(40))));
    }

    @Test void quantidadeValidaAcimaDeUmMilhaoNaoTemLimiteArtificial() throws Exception {
        long cliente = cliente(), produto = produto();
        criar("/api/estoque/entradas", Map.of("produtoId", produto, "quantidade", 1000001));
        var pedido = criar("/api/pedidos", pedido(cliente, produto, 1000001));
        assertThat(pedido.get("itens").get(0).get("quantidade").asInt()).isEqualTo(1000001);
        mvc.perform(get("/api/estoque/" + produto).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.reservado").value(1000001)).andExpect(jsonPath("$.disponivel").value(0));
    }

    @Test void naoExpoeHashesDeSenha() throws Exception {
        var result = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("senha", "$2a$", "$2b$");
    }
}
