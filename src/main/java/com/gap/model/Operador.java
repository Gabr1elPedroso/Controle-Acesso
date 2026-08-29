package com.gap.model;

public class Operador {
    private int id;
    private String nome;
    private String login;
    private String nivel;

    public Operador(int id, String nome, String login, String nivel) {
        this.id = id;
        this.nome = nome;
        this.login = login;
        this.nivel = nivel;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getLogin() { return login; }
    public String getNivel() { return nivel; }
}