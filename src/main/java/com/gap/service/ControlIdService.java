package com.gap.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ControlIdService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * PASSO 1: Fazer Login em um equipamento específico pelo IP
     */
    public String fazerLogin(String ipEquipamento, String usuario, String senha) throws Exception {
        String url = "http://" + ipEquipamento + "/login.fcgi";

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
     * PASSO 2: Criar Usuário em um equipamento dinâmico
     * @return O ID numérico gerado pelo iDFace para este leitor
     */
    public long criarUsuario(String ipEquipamento, String sessionToken, String nome, String matricula) throws Exception {
        String url = "http://" + ipEquipamento + "/create_objects.fcgi?session=" + sessionToken;

        JsonObject objUser = new JsonObject();
        objUser.addProperty("name", nome);
        objUser.addProperty("registration", matricula);

        JsonArray values = new JsonArray();
        values.add(objUser);

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("object", "users");
        jsonBody.add("values", values);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            long idGerado = jsonResponse.getAsJsonArray("ids").get(0).getAsLong();
            System.out.println("Usuário '" + nome + "' criado no leitor (" + ipEquipamento + ") com ID: " + idGerado);
            return idGerado;
        } else {
            throw new Exception("Erro ao criar usuário no leitor " + ipEquipamento + ": " + response.body());
        }
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
    public void enviarFoto(String ipEquipamento,
                       String sessionToken,
                       long idUsuarioAparelho,
                       String fotoBase64) throws Exception {

    // Timestamp Unix em segundos
    long timestamp = System.currentTimeMillis() / 1000L;

    String url = "http://" + ipEquipamento
            + "/user_set_image.fcgi"
            + "?user_id=" + idUsuarioAparelho
            + "&timestamp=" + timestamp
            + "&match=0"
            + "&session=" + sessionToken;

    byte[] imagemBytes = java.util.Base64.getDecoder().decode(fotoBase64);

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(imagemBytes))
            .build();

    HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("URL: " + url);
    System.out.println("Status: " + response.statusCode());
    System.out.println("Resposta: " + response.body());

    if (response.statusCode() != 200) {
        throw new Exception("Erro ao enviar foto para o leitor "
                + ipEquipamento + ": " + response.body());
    }

    System.out.println("Foto enviada com sucesso!");
}

    /**
     * PASSO 4: Excluir Usuário de um equipamento específico
     */
    public void excluirUsuario(String ipEquipamento, String sessionToken, long idUsuarioAparelho) throws Exception {
        String url = "http://" + ipEquipamento + "/destroy_objects.fcgi?session=" + sessionToken;

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
}