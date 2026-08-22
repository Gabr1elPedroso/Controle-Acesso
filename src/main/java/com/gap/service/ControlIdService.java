package com.gap.service;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ControlIdService {

    private final String IP_APARELHO = "http://192.168.1.50";
    private String sessionToken = "";
    private final HttpClient httpClient;


    public ControlIdService(){
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean fazerLogin(String usuario, String senha) throws Exception{
        String url = IP_APARELHO + "/login.fcgi";

        JsonObject jsonLogin = new JsonObject();
        jsonLogin.addProperty("login", usuario);
        jsonLogin.addProperty("password", senha);

        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonLogin.toString()))
        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200){
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            this.sessionToken = jsonResponse.get("session").getAsString();
            System.out.println("Login realizado com sucesso. Token: " + this.sessionToken);
            return true;
        } else{
            System.out.println("Falha no login. Código: " + response.statusCode());
            return false;
        }
    }

        public long criarUsuario(String nome, String matricula) throws Exception{
            if (sessionToken.isEmpty()) throw new Exception("Voce precisa fazer login primeiro");

            String url = IP_APARELHO + "/create_objects.fcgi?session=" + sessionToken;

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

            if (response.statusCode() == 200){

                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                long idGerado = jsonResponse.getAsJsonArray("ids").get(0).getAsLong();

                System.out.println("Usuario" + nome + "criado no IDFace com ID:" + idGerado);
                return idGerado;
            } else {
                throw new Exception("Falha ao criar usuario" + response.body());
            }
            
        }

        public void enviarFoto(long idUsuario, String fotoBase64) throws Exception{
            if (sessionToken.isEmpty()) throw new Exception("Voce precisa fazer login primeiro");

            String url = IP_APARELHO + "/user_set_image_fcgi?session=" + sessionToken;
            
            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("user_id", idUsuario);
            jsonBody.addProperty("image", "data:image/jpeg;base64," + fotoBase64);


            HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
             .header("Content-Type", "application/json")
             .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
             .build();

             HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

             if (response.statusCode()== 200){
                System.out.println("Foto enviada com sucesso para o usuario ID:" + idUsuario);
             } else {
                System.out.println("Falha ao enviar foto para o usuario ID:" + idUsuario + " - " + response.body());
             }
        }

    }


