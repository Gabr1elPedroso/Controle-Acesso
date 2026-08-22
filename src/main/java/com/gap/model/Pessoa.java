package com.gap.model;

public class Pessoa {
    private int id;
    private String nome;
    private String matricula;
    private int categoriaId;
    private String fotoBase64;

    public Pessoa(int id, String nome, String matricula, int categoriaId, String fotoBase64) {
        this.id = id;
        this.nome = nome;
        this.matricula = matricula;
        this.categoriaId = categoriaId;
        this.fotoBase64 = fotoBase64;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getMatricula() { return matricula; }
    public int getCategoriaId() { return categoriaId; }
    public String getFotoBase64() { return fotoBase64; }
}