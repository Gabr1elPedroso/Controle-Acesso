package com.gap.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.gap.banco.ConexaoSQLite;
import com.gap.model.Categoria;
import com.gap.model.Equipamento;
import com.gap.model.Pessoa;

/**
 * Orquestra a sincronização de todas as pessoas do banco local
 * com todos os equipamentos cadastrados, respeitando a restrição
 * por categoria configurada em cada equipamento.
 *
 * Estratégia por equipamento:
 *   1. Login
 *   2. Garante a regra de acesso "Acesso Total" (24h/todos os dias,
 *      vinculada a todos os portais) — criada uma única vez, cacheada
 *      localmente.
 *   3. Para cada pessoa, verifica se a categoria dela é permitida nesse
 *      equipamento:
 *      - Se não é permitida: remove do equipamento se já estava
 *        sincronizada antes; senão, apenas pula.
 *      - Se é permitida: delete (se existia) -> create -> vincula ao
 *        grupo da categoria (criado sob demanda) -> envia foto -> salva
 *        o mapeamento local.
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

        Map<Integer, Categoria> categoriasPorId = new HashMap<>();
        for (Categoria c : ConexaoSQLite.listarCategorias()) {
            categoriasPorId.put(c.getId(), c);
        }

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
                        equip.getIp(), equip.getporta(), equip.getUsuario(), equip.getSenha());
                log.accept("  ✔ Login realizado.", false);
            } catch (Exception e) {
                log.accept("  ✖ Falha no login: " + e.getMessage()
                        + " — equipamento ignorado.", true);
                houveErro[0] = true;
                feitos[0] += pessoas.size();
                progresso.accept((double) feitos[0] / total);
                continue;
            }

            // --- Garante a regra "Acesso Total" (criada uma única vez) ---
            Long accessRuleId = ConexaoSQLite.getAccessRuleIdDispositivo(equip.getId());
            if (accessRuleId == null) {
                try {
                    accessRuleId = controlIdService.garantirRegraAcessoTotal(
                            equip.getIp(), equip.getporta(), token);
                    ConexaoSQLite.salvarAccessRuleIdDispositivo(equip.getId(), accessRuleId);
                    log.accept("  ✔ Regra de acesso 'Acesso Total' criada neste equipamento.", false);
                } catch (Exception e) {
                    log.accept("  ✖ Falha ao criar regra de acesso: " + e.getMessage()
                            + " — equipamento ignorado.", true);
                    houveErro[0] = true;
                    feitos[0] += pessoas.size();
                    progresso.accept((double) feitos[0] / total);
                    continue;
                }
            }

            // --- Categorias permitidas neste equipamento (null = todas) ---
            Set<Integer> permitidas = ConexaoSQLite.categoriasPermitidas(equip.getId());
            if (permitidas != null) {
                log.accept("  · Restrito por categoria (" + permitidas.size() + " categoria(s) permitida(s)).", false);
            }

            // Cache local (nesta rodada) de grupo-por-categoria já
            // garantido neste equipamento, para não checar/criar de novo
            // a cada pessoa.
            Map<Integer, Long> gruposGarantidos = new HashMap<>();

            // --- Pessoas ---
            for (Pessoa pessoa : pessoas) {

                Long idAnterior = ConexaoSQLite
                        .buscarIdNoEquipamento(pessoa.getId(), equip.getId());

                boolean permitido = (permitidas == null) || permitidas.contains(pessoa.getCategoriaId());

                if (!permitido) {
                    if (idAnterior != null) {
                        try {
                            controlIdService.excluirUsuario(equip.getIp(), equip.getporta(), token, idAnterior);
                            ConexaoSQLite.removerMapeamentoEquipamento(pessoa.getId(), equip.getId());
                            log.accept("  · " + pessoa.getNome() + " — removido (categoria não autorizada neste equipamento).", false);
                        } catch (Exception e) {
                            log.accept("  ⚠ " + pessoa.getNome() + " — falha ao remover: " + e.getMessage(), false);
                        }
                    } else {
                        log.accept("  · " + pessoa.getNome() + " — pulado (categoria não autorizada neste equipamento).", false);
                    }
                    feitos[0]++;
                    progresso.accept((double) feitos[0] / total);
                    continue;
                }

                log.accept("  · " + pessoa.getNome() + " (" + pessoa.getMatricula() + ")", false);

                // 1. Delete (ignora falha — pode não existir ainda)
                if (idAnterior != null) {
                    try {
                        controlIdService.excluirUsuario(equip.getIp(), equip.getporta(), token, idAnterior);
                        log.accept("    ↺ Registro anterior removido.", false);
                    } catch (Exception e) {
                        log.accept("    ⚠ Não encontrado no equipamento; prosseguindo.", false);
                    }
                }

                // 2. Criar
                long novoId;
                try {
                    novoId = controlIdService.criarUsuario(
                            equip.getIp(), equip.getporta(), token, pessoa.getNome(), pessoa.getMatricula());
                    log.accept("    ✔ Criado com ID " + novoId + ".", false);
                } catch (Exception e) {
                    log.accept("    ✖ Erro ao criar: " + e.getMessage(), true);
                    houveErro[0] = true;
                    feitos[0]++;
                    progresso.accept((double) feitos[0] / total);
                    continue;
                }

                // 3. Vincula ao grupo da categoria (cria o grupo sob
                // demanda, na primeira vez que essa categoria aparece
                // neste equipamento)
                int categoriaId = pessoa.getCategoriaId();
                Long grupoId = gruposGarantidos.get(categoriaId);
                if (grupoId == null) {
                    grupoId = ConexaoSQLite.getGrupoIdDispositivo(equip.getId(), categoriaId);
                    if (grupoId == null) {
                        try {
                            String nomeCategoria = categoriasPorId.containsKey(categoriaId)
                                    ? categoriasPorId.get(categoriaId).getNome()
                                    : ("Categoria " + categoriaId);
                            grupoId = controlIdService.criarGrupo(equip.getIp(), equip.getporta(), token, nomeCategoria);
                            controlIdService.vincularGrupoRegra(equip.getIp(), equip.getporta(), token, grupoId, accessRuleId);
                            ConexaoSQLite.salvarGrupoIdDispositivo(equip.getId(), categoriaId, grupoId);
                            log.accept("    ✔ Grupo '" + nomeCategoria + "' criado e vinculado à regra de acesso.", false);
                        } catch (Exception e) {
                            log.accept("    ✖ Erro ao preparar grupo da categoria: " + e.getMessage(), true);
                            houveErro[0] = true;
                            grupoId = null;
                        }
                    }
                    if (grupoId != null) {
                        gruposGarantidos.put(categoriaId, grupoId);
                    }
                }

                if (grupoId != null) {
                    try {
                        controlIdService.vincularUsuarioGrupo(equip.getIp(), equip.getporta(), token, novoId, grupoId);
                    } catch (Exception e) {
                        log.accept("    ⚠ Falha ao vincular ao grupo (acesso pode ficar negado): " + e.getMessage(), true);
                        houveErro[0] = true;
                    }
                }

                // 4. Foto (opcional — não interrompe se falhar)
                if (pessoa.getFotoBase64() != null && !pessoa.getFotoBase64().isEmpty()) {
                    try {
                        controlIdService.enviarFoto(
                                equip.getIp(), equip.getporta(), token, novoId, pessoa.getFotoBase64());
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