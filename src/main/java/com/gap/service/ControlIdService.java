package com.gap.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ControlIdService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String montarUrl(String ipEquipamento, int porta, String caminho) {
        return "http://" + ipEquipamento + ":" + porta + caminho;
    }

    /**
     * PASSO 1: Fazer Login em um equipamento específico pelo IP.
     */
    public String fazerLogin(String ipEquipamento, int porta, String usuario, String senha) throws Exception {
        String url = montarUrl(ipEquipamento, porta, "/login.fcgi");

        JsonObject jsonLogin = new JsonObject();
        jsonLogin.addProperty("login", usuario);
        jsonLogin.addProperty("password", senha);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonLogin.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            String sessionToken = jsonResponse.get("session").getAsString();
            System.out.println("Login bem-sucedido no IP " + ipEquipamento + "! Token: " + sessionToken);
            return sessionToken;
        } else {
            throw new Exception("Falha no login para o IP " + ipEquipamento + ". Código HTTP: " + response.statusCode());
        }
    }

    /**
     * PASSO 2: Criar Usuário em um equipamento dinâmico.
     * @return O ID numérico gerado pelo iDFace para este leitor
     */
    public long criarUsuario(String ipEquipamento, int porta, String sessionToken, String nome, String matricula) throws Exception {
        JsonObject objUser = new JsonObject();
        objUser.addProperty("name", nome);
        objUser.addProperty("registration", matricula);

        JsonArray values = new JsonArray();
        values.add(objUser);

        long idGerado = createObjects(ipEquipamento, porta, sessionToken, "users", values).get(0).getAsLong();
        System.out.println("Usuário '" + nome + "' criado no leitor (" + ipEquipamento + ") com ID: " + idGerado);
        return idGerado;
    }

    /**
     * PASSO 3: Enviar Foto para um equipamento Control iD.
     *
     * Confirmado na documentação oficial (controlid.com.br/docs/access-api):
     * ao contrário da maioria dos comandos dessa API, este exige
     * Content-Type: application/octet-stream, com os bytes crus da
     * imagem no corpo do POST — o user_id vai na Query String, não no body.
     * Limite: foto deve ter menos de 1MB (algumas versões de firmware
     * aceitam até 2MB).
     */
    public void enviarFoto(String ipEquipamento, int porta, String sessionToken, long idUsuarioAparelho, String fotoBase64) throws Exception {
        String url = montarUrl(ipEquipamento, porta, "/user_set_image.fcgi")
                + "?user_id=" + idUsuarioAparelho
                + "&session=" + sessionToken;

        byte[] imagemBytes = java.util.Base64.getDecoder().decode(fotoBase64);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(imagemBytes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Foto associada com sucesso ao ID " + idUsuarioAparelho + " no leitor (" + ipEquipamento + ")");
        } else {
            throw new Exception("Erro ao enviar foto para o leitor " + ipEquipamento + ": " + response.body());
        }
    }

    /**
     * PASSO 4: Excluir Usuário de um equipamento específico.
     */
    public void excluirUsuario(String ipEquipamento, int porta, String sessionToken, long idUsuarioAparelho) throws Exception {
        String url = montarUrl(ipEquipamento, porta, "/destroy_objects.fcgi") + "?session=" + sessionToken;

        JsonObject jsonId = new JsonObject();
        jsonId.addProperty("id", idUsuarioAparelho);

        JsonObject jsonUsers = new JsonObject();
        jsonUsers.add("users", jsonId);

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("object", "users");
        jsonBody.add("where", jsonUsers);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Usuário ID " + idUsuarioAparelho + " removido com sucesso do leitor (" + ipEquipamento + ")");
        } else {
            throw new Exception("Erro ao excluir usuário do leitor " + ipEquipamento + ": " + response.body());
        }
    }

    // ==========================================================
    // OBJETOS GENÉRICOS (create_objects.fcgi / load_objects.fcgi)
    // ==========================================================

    /**
     * Cria um ou mais objetos de um tipo qualquer da API do Control iD
     * (groups, access_rules, time_zones, time_spans, user_groups,
     * group_access_rules, access_rule_time_zones, portal_access_rules...).
     * @return o array de IDs gerados, na mesma ordem de "values"
     */
    private JsonArray createObjects(String ipEquipamento, int porta, String sessionToken, String object, JsonArray values) throws Exception {
        String url = montarUrl(ipEquipamento, porta, "/create_objects.fcgi") + "?session=" + sessionToken;

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("object", object);
        jsonBody.add("values", values);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray("ids");
        } else {
            throw new Exception("Erro ao criar objeto '" + object + "' no leitor " + ipEquipamento + ": " + response.body());
        }
    }

    /**
     * Carrega todos os objetos de um tipo (sem filtro) — usado aqui só
     * para listar os portais existentes no equipamento.
     */
    private JsonArray loadObjects(String ipEquipamento, int porta, String sessionToken, String object) throws Exception {
        String url = montarUrl(ipEquipamento, porta, "/load_objects.fcgi") + "?session=" + sessionToken;

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("object", object);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray(object);
        } else {
            throw new Exception("Erro ao carregar objetos '" + object + "' no leitor " + ipEquipamento + ": " + response.body());
        }
    }

    public List<Long> listarPortais(String ipEquipamento, int porta, String sessionToken) throws Exception {
        JsonArray portais = loadObjects(ipEquipamento, porta, sessionToken, "portals");
        List<Long> ids = new ArrayList<>();
        for (JsonElement el : portais) {
            ids.add(el.getAsJsonObject().get("id").getAsLong());
        }
        return ids;
    }

    // ==========================================================
    // GRUPOS, REGRAS DE ACESSO E HORÁRIOS
    // (fluxo recomendado pela Control iD: categoria local -> grupo no
    // equipamento -> regra de acesso -> horário + portais)
    // ==========================================================

    public long criarGrupo(String ipEquipamento, int porta, String sessionToken, String nome) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", nome);

        JsonArray values = new JsonArray();
        values.add(obj);

        long id = createObjects(ipEquipamento, porta, sessionToken, "groups", values).get(0).getAsLong();
        System.out.println("Grupo '" + nome + "' criado no leitor (" + ipEquipamento + ") com ID: " + id);
        return id;
    }

    public long criarRegraAcesso(String ipEquipamento, int porta, String sessionToken, String nome) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", nome);

        JsonArray values = new JsonArray();
        values.add(obj);

        long id = createObjects(ipEquipamento, porta, sessionToken, "access_rules", values).get(0).getAsLong();
        System.out.println("Regra de acesso '" + nome + "' criada no leitor (" + ipEquipamento + ") com ID: " + id);
        return id;
    }

    /**
     * Cria um horário (time_zone) com um único intervalo (time_span)
     * cobrindo todos os dias da semana, das 00:00 às 24:00.
     * @return o ID do time_zone criado
     */
    private long criarHorarioAcessoTotal(String ipEquipamento, int porta, String sessionToken) throws Exception {
        JsonObject tz = new JsonObject();
        tz.addProperty("name", "Acesso Total (GAP)");
        JsonArray tzValues = new JsonArray();
        tzValues.add(tz);
        long timeZoneId = createObjects(ipEquipamento, porta, sessionToken, "time_zones", tzValues).get(0).getAsLong();

        // Intervalo único: todos os dias, dia inteiro (0 a 86400 segundos
        // desde a meia-noite). hol1/hol2/hol3 são flags de feriado — '0'
        // significa que feriados seguem a mesma regra dos outros dias.
        JsonObject span = new JsonObject();
        span.addProperty("time_zone_id", timeZoneId);
        span.addProperty("start", "0");
        span.addProperty("end", "86400");
        span.addProperty("sun", "1");
        span.addProperty("mon", "1");
        span.addProperty("tue", "1");
        span.addProperty("wed", "1");
        span.addProperty("thu", "1");
        span.addProperty("fri", "1");
        span.addProperty("sat", "1");
        span.addProperty("hol1", "0");
        span.addProperty("hol2", "0");
        span.addProperty("hol3", "0");
        JsonArray spanValues = new JsonArray();
        spanValues.add(span);
        createObjects(ipEquipamento, porta, sessionToken, "time_spans", spanValues);

        System.out.println("Horário 'Acesso Total' criado no leitor (" + ipEquipamento + ") com ID: " + timeZoneId);
        return timeZoneId;
    }

    public void vincularGrupoRegra(String ipEquipamento, int porta, String sessionToken, long grupoId, long regraId) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("group_id", grupoId);
        obj.addProperty("access_rule_id", regraId);
        JsonArray values = new JsonArray();
        values.add(obj);
        createObjects(ipEquipamento, porta, sessionToken, "group_access_rules", values);
    }

    private void vincularHorarioRegra(String ipEquipamento, int porta, String sessionToken, long timeZoneId, long regraId) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("time_zone_id", timeZoneId);
        obj.addProperty("access_rule_id", regraId);
        JsonArray values = new JsonArray();
        values.add(obj);
        createObjects(ipEquipamento, porta, sessionToken, "access_rule_time_zones", values);
    }

    private void vincularPortalRegra(String ipEquipamento, int porta, String sessionToken, long portalId, long regraId) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("portal_id", portalId);
        obj.addProperty("access_rule_id", regraId);
        JsonArray values = new JsonArray();
        values.add(obj);
        createObjects(ipEquipamento, porta, sessionToken, "portal_access_rules", values);
    }

    public void vincularUsuarioGrupo(String ipEquipamento, int porta, String sessionToken, long userId, long grupoId) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("user_id", userId);
        obj.addProperty("group_id", grupoId);
        JsonArray values = new JsonArray();
        values.add(obj);
        createObjects(ipEquipamento, porta, sessionToken, "user_groups", values);
    }

    /**
     * Garante que o equipamento tenha uma regra de acesso "Acesso Total"
     * (24h, todos os dias, vinculada a todos os portais do equipamento) e
     * retorna o ID dessa regra. Só chame quando ainda não tiver sido criada
     * — verifique ConexaoSQLite.getAccessRuleIdDispositivo antes, e salve
     * o retorno com salvarAccessRuleIdDispositivo depois, para não recriar
     * a regra a cada sincronização.
     */
    public long garantirRegraAcessoTotal(String ipEquipamento, int porta, String sessionToken) throws Exception {
        long timeZoneId = criarHorarioAcessoTotal(ipEquipamento, porta, sessionToken);
        long regraId = criarRegraAcesso(ipEquipamento, porta, sessionToken, "Acesso Total (GAP)");
        vincularHorarioRegra(ipEquipamento, porta, sessionToken, timeZoneId, regraId);

        List<Long> portais = listarPortais(ipEquipamento, porta, sessionToken);
        for (Long portalId : portais) {
            vincularPortalRegra(ipEquipamento, porta, sessionToken, portalId, regraId);
        }

        return regraId;
    }
}