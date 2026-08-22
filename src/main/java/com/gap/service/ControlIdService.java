package com.gap.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

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
     * PASSO 3: Enviar Foto em Base64 para um equipamento dinâmico
     */
    public void enviarFoto(String ipEquipamento, String sessionToken, long idUsuarioAparelho, String fotoBase64) throws Exception {
        String url = "http://" + ipEquipamento + "/user_set_image.fcgi?session=" + sessionToken;

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("user_id", idUsuarioAparelho);
        jsonBody.addProperty("image", "data:image/jpeg;base64," + fotoBase64);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Foto associada com sucesso ao ID " + idUsuarioAparelho + " no leitor (" + ipEquipamento + ")");
        } else {
            throw new Exception("Erro ao enviar foto para o leitor " + ipEquipamento + ": " + response.body());
        }
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