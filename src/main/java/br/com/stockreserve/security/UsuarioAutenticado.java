package br.com.stockreserve.security;

import br.com.stockreserve.domain.Perfil;
import java.security.Principal;

public record UsuarioAutenticado(long id, String email, Perfil perfil) implements Principal {
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Override public String getName() { return email; }
}
