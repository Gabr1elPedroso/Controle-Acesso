package com.gap.service;

import java.util.List;
import java.util.function.BiConsumer;

import com.gap.banco.ConexaoSQLite;
import com.gap.model.Equipamento;
import com.gap.model.Pessoa;

/**
 * Orquestra a sincronização de todas as pessoas do banco local
 * com todos os equipamentos cadastrados.
 *
 * Estratégia por pessoa, por equipamento:
 *   1. Login  → obtém session token
 *   2. Delete → tenta remover a pessoa se já existir (ignora erro se não existia)
 *   3. Create → cria a pessoa e obtém o ID gerado pelo equipamento
 *   4. Foto   → envia a foto (se existir)
 *   5. Salva  → persiste o mapeamento (id_local ↔ id_equipamento) no banco
 *
 * O callback `log` recebe mensagens de progresso em tempo real para
 * a UI mostrar no TextArea sem bloquear a thread de eventos do JavaFX.
 */
public class SincronizacaoService {

    private final ControlIdService controlIdService = new ControlIdService();

    /**
     * @param log       callback chamado a cada passo — (mensagem, erro?)
     * @param progresso callback chamado com o percentual atual (0.0 a 1.0)
     * @return true se não houve nenhuma falha crítica
     */
    public boolean sincronizarTudo(
            BiConsumer<String, Boolean> log,
            java.util.function.DoubleConsumer progresso) {

        List<Pessoa> pessoas = ConexaoSQLite.listarUsuarios();
        List<Equipamento> equipamentos = ConexaoSQLite.listarEquipamentos();

        if (equipamentos.isEmpty()) {
            log.accept("Nenhum equipamento cadastrado. Cadastre ao menos um equipamento antes de sincronizar.", true);
            return false;
        }

        if (pessoas.isEmpty()) {
            log.accept("Nenhuma pessoa cadastrada. Nada a sincronizar.", false);
            return true;
        }

        int total = pessoas.size() * equipamentos.size();
        int[] feitos = {0};
        boolean[] houveErro = {false};

        log.accept("Iniciando sincronização: "
                + pessoas.size() + " pessoa(s) × "
                + equipamentos.size() + " equipamento(s).", false);
        log.accept("─".repeat(55), false);

        for (Equipamento equip : equipamentos) {

            log.accept("\n► Equipamento: " + equip.getNome() + " (" + equip.getIp() + ")", false);

            // --- Login ---
            String token;
            try {
                token = controlIdService.fazerLogin(
                        equip.getIp(), equip.getUsuario(), equip.getSenha());
                log.accept("  ✔ Login realizado.", false);
            } catch (Exception e) {
                log.accept("  ✖ Falha no login: " + e.getMessage()
                        + " — equipamento ignorado.", true);
                houveErro[0] = true;
                feitos[0] += pessoas.size();
                progresso.accept((double) feitos[0] / total);
                continue;
            }

            // --- Pessoas ---
            for (Pessoa pessoa : pessoas) {

                log.accept("  · " + pessoa.getNome() + " (" + pessoa.getMatricula() + ")", false);

                // 1. Busca ID anterior no equipamento (se já foi sincronizado antes)
                Long idAnterior = ConexaoSQLite
                        .buscarIdNoEquipamento(pessoa.getId(), equip.getId());

                // 2. Delete (ignora falha — pode não existir ainda)
                if (idAnterior != null) {
                    try {
                        controlIdService.excluirUsuario(equip.getIp(), token, idAnterior);
                        log.accept("    ↺ Registro anterior removido.", false);
                    } catch (Exception e) {
                        log.accept("    ⚠ Não encontrado no equipamento; prosseguindo.", false);
                    }
                }

                // 3. Criar
                long novoId;
                try {
                    novoId = controlIdService.criarUsuario(
                            equip.getIp(), token, pessoa.getNome(), pessoa.getMatricula());
                    log.accept("    ✔ Criado com ID " + novoId + ".", false);
                } catch (Exception e) {
                    log.accept("    ✖ Erro ao criar: " + e.getMessage(), true);
                    houveErro[0] = true;
                    feitos[0]++;
                    progresso.accept((double) feitos[0] / total);
                    continue;
                }

                // 4. Foto (opcional — não interrompe se falhar)
                if (pessoa.getFotoBase64() != null && !pessoa.getFotoBase64().isEmpty()) {
                    try {
                        controlIdService.enviarFoto(
                                equip.getIp(), token, novoId, pessoa.getFotoBase64());
                        log.accept("    ✔ Foto enviada.", false);
                    } catch (Exception e) {
                        log.accept("    ⚠ Foto não enviada: " + e.getMessage(), false);
                    }
                } else {
                    log.accept("    – Sem foto cadastrada.", false);
                }

                // 5. Persiste o mapeamento
                ConexaoSQLite.salvarMapeamentoEquipamento(
                        pessoa.getId(), equip.getId(), novoId);

                feitos[0]++;
                progresso.accept((double) feitos[0] / total);
            }
        }

        log.accept("\n" + "─".repeat(55), false);

        if (houveErro[0]) {
            log.accept("Sincronização concluída com erros. Revise os itens marcados com ✖.", true);
        } else {
            log.accept("✔ Sincronização concluída com sucesso!", false);
        }

        return !houveErro[0];
    }
}