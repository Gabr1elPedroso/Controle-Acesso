package com.gap.model;

public class Equipamento {
    private int id;
    private String nome;
    private String ip;

    public Equipamento(int id, String nome, String ip) {
        this.id = id;
        this.nome = nome;
        this.ip = ip;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getIp() { return ip; }
}