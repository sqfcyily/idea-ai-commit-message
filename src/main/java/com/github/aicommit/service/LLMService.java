package com.github.aicommit.service;

import com.github.aicommit.settings.AICommitPasswordSafe;
import com.github.aicommit.settings.AICommitSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class LLMService {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(90))
            .build();

    public static String generateCommitMessage(String diffText) throws Exception {
        AICommitSettings.State settings = AICommitSettings.getInstance().getState();
        if (settings == null) {
            throw new RuntimeException("Settings not initialized");
        }

        String apiKey = AICommitPasswordSafe.getApiKey();
        if (apiKey == null) apiKey = "";
        
        AICommitSettings.Provider provider = AICommitSettings.Provider.valueOf(settings.provider);
        String baseUrl = settings.apiUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        String model = settings.modelName.trim();
        String language = settings.language;

        // Build prompt from template
        String prompt = settings.promptTemplate
                .replace("{diff}", diffText)
                .replace("{language}", language);

        String url;
        String jsonBody;
        java.util.Map<String, String> headers = new java.util.HashMap<>();

        switch (provider) {
            case GEMINI:
                url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                JsonObject geminiBody = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject contentObj = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject partObj = new JsonObject();
                partObj.addProperty("text", prompt);
                parts.add(partObj);
                contentObj.add("parts", parts);
                contents.add(contentObj);
                geminiBody.add("contents", contents);
                jsonBody = geminiBody.toString();
                break;

            case OPENAI:
            case CUSTOM:
                url = baseUrl + "/chat/completions";
                JsonObject openaiBody = new JsonObject();
                openaiBody.addProperty("model", model);
                JsonArray messages = new JsonArray();
                JsonObject messageObj = new JsonObject();
                messageObj.addProperty("role", "user");
                messageObj.addProperty("content", prompt);
                messages.add(messageObj);
                openaiBody.add("messages", messages);
                jsonBody = openaiBody.toString();
                if (!apiKey.isEmpty()) {
                    headers.put("Authorization", "Bearer " + apiKey);
                }
                break;

            case OLLAMA:
                url = baseUrl + "/api/generate";
                JsonObject ollamaBody = new JsonObject();
                ollamaBody.addProperty("model", model);
                ollamaBody.addProperty("prompt", prompt);
                ollamaBody.addProperty("stream", false);
                jsonBody = ollamaBody.toString();
                break;

            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        headers.forEach(requestBuilder::header);

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMsg;
            try {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                errorMsg = json.toString();
            } catch (Exception e) {
                errorMsg = response.body();
            }
            throw new RuntimeException("API Request failed (HTTP " + response.statusCode() + "): " + errorMsg);
        }

        return parseResponse(provider, response.body());
    }

    private static String parseResponse(AICommitSettings.Provider provider, String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        String text;
        switch (provider) {
            case GEMINI:
                JsonArray candidates = root.getAsJsonArray("candidates");
                if (candidates == null || candidates.isEmpty()) {
                    throw new RuntimeException("No candidates returned from Gemini API");
                }
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts == null || parts.isEmpty()) {
                    throw new RuntimeException("No content parts returned from Gemini API");
                }
                text = parts.get(0).getAsJsonObject().get("text").getAsString();
                break;

            case OPENAI:
            case CUSTOM:
                JsonArray choices = root.getAsJsonArray("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new RuntimeException("No choices returned from Chat API");
                }
                JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                text = message.get("content").getAsString();
                break;

            case OLLAMA:
                text = root.get("response").getAsString();
                break;

            default:
                throw new IllegalArgumentException("Unknown provider: " + provider);
        }
        return text.trim();
    }
}
