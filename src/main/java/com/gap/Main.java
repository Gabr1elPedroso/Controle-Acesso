package com.gap;

import com.gap.banco.ConexaoSQLite;

public class Main {
    public static void main(String[] args) {
        System.out.println("Sistema de Controle de Acesso");
    
       ConexaoSQLite.criarTabela();
    }
}
