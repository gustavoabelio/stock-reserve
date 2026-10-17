package br.com.stockreserve.domain;

public record Usuario(long id, String nome, String email, String senhaHash, Perfil perfil) {}
