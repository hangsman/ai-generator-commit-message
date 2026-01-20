package com.github.jdami.aicommit.service.provider;

import com.github.jdami.aicommit.service.model.GenerationInputs;
import com.github.jdami.aicommit.service.util.CommitMessageCleaner;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

/**
 * Provider client for OpenRouter API.
 * OpenRouter uses OpenAI-compatible API format.
 */
public class OpenRouterProviderClient extends BaseHttpProviderClient {

    @Override
    public String generate(GenerationInputs inputs, ProgressIndicator indicator) throws IOException {
        var client = buildClient(inputs);

        com.google.gson.JsonObject systemMessage = new com.google.gson.JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", inputs.systemPrompt);

        com.google.gson.JsonObject userMessage = new com.google.gson.JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", inputs.prompt);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
        requestBody.addProperty("model", inputs.model);
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);

        String url;
        if (inputs.endpoint.endsWith("#")) {
            url = inputs.endpoint.substring(0, inputs.endpoint.length() - 1);
        } else {
            url = normalizeBaseUrl(inputs.endpoint) + "/v1/chat/completions";
        }
        String jsonBody = gson.toJson(requestBody);

        System.out.println("=== OpenRouter Request ===");
        System.out.println("Endpoint: " + url);
        System.out.println("Model: " + inputs.model);
        System.out.println("System Prompt: " + inputs.systemPrompt);
        System.out.println("User Prompt: " + inputs.prompt);
        System.out.println("Full Request Body: " + jsonBody);
        System.out.println("==========================");

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + inputs.apiKey)
                .addHeader("HTTP-Referer", "https://github.com/jdami/ai-generator-commit-message")
                .addHeader("X-Title", "AI Commit Message Generator")
                .build();

        Call call = client.newCall(request);
        ongoingCall = call;

        try {
            checkCanceled(indicator);

            try (Response response = call.execute()) {
                checkCanceled(indicator);

                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (!response.isSuccessful()) {
                    System.err.println("=== OpenRouter Error Response ===");
                    System.err.println("HTTP Status: " + response.code() + " " + response.message());
                    System.err.println("Response Body: " + responseBody);
                    System.err.println("Request Body Size: " + jsonBody.length() + " chars");
                    System.err.println("=================================");
                    
                    // Try to extract error message from response
                    String errorDetail = "未知错误";
                    String suggestion = "";
                    try {
                        var errorJson = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                        if (errorJson.has("error")) {
                            var error = errorJson.getAsJsonObject("error");
                            String mainMessage = error.has("message") ? error.get("message").getAsString() : "";
                            
                            // Try to extract nested error from metadata.raw
                            if (error.has("metadata") && error.getAsJsonObject("metadata").has("raw")) {
                                String rawError = error.getAsJsonObject("metadata").get("raw").getAsString();
                                try {
                                    var rawJson = gson.fromJson(rawError, com.google.gson.JsonObject.class);
                                    if (rawJson.has("message")) {
                                        errorDetail = rawJson.get("message").getAsString();
                                    }
                                } catch (Exception e) {
                                    errorDetail = rawError;
                                }
                            } else {
                                errorDetail = mainMessage;
                            }
                            
                            // Add suggestion for common errors
                            if (errorDetail.contains("exceed") || errorDetail.contains("token") || errorDetail.contains("length")) {
                                suggestion = "\n\n💡 建议：请在设置中选择更小的\"上下文窗口\"预设，或减少提交的文件数量。";
                            }
                        }
                    } catch (Exception ignored) {
                        errorDetail = responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
                    }
                    
                    throw new IOException("API 错误 (" + response.code() + "): " + errorDetail + suggestion);
                }

                System.out.println("=== OpenRouter Response ===");
                System.out.println("Raw Response: " + responseBody);

                var jsonResponse = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()
                        && jsonResponse.getAsJsonArray("choices").size() > 0) {
                    var firstChoice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();
                    var message = firstChoice.has("message") ? firstChoice.getAsJsonObject("message") : null;
                    String rawResponse = message != null && message.has("content")
                            ? message.get("content").getAsString().trim()
                            : null;

                    if (rawResponse == null) {
                        throw new IOException("Invalid response format from OpenRouter: missing content");
                    }

                    System.out.println("Raw Message: " + rawResponse);

                    String finalMessage = CommitMessageCleaner.clean(rawResponse);
                    System.out.println("Final Message: " + finalMessage);
                    System.out.println("===========================");

                    return finalMessage;
                } else {
                    String errorMsg = "Invalid response format from OpenRouter";
                    System.err.println("OpenRouter Error: " + errorMsg);
                    throw new IOException(errorMsg);
                }
            }
        } catch (ProcessCanceledException canceled) {
            throw canceled;
        } catch (IOException ex) {
            if (indicator != null && indicator.isCanceled()) {
                throw new ProcessCanceledException();
            }
            throw ex;
        } finally {
            ongoingCall = null;
        }
    }
}
