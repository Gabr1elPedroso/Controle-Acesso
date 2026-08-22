package com.gap.model;

public class Equipamento {
    private int id;
    private String nome;
    private String ip;
    private String usuario;
    private String senha;
    private String status;
    private int porta;

    public Equipamento(int id, String nome, String ip,int porta, String usuario, String senha, String status) {
        this.id = id;
        this.nome = nome;
        this.ip = ip;
        this.porta = porta;
        this.usuario = usuario;
        this.senha = senha;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getIp() { return ip; }
    public int getporta() {return porta;}
    public String getUsuario() { return usuario; }
    public String getSenha() { return senha; }
    public String getStatus() { return status; }
}