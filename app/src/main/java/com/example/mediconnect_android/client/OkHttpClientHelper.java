package com.example.mediconnect_android.client;

import com.example.mediconnect_android.client.response.ApiGenericResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpClientHelper {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public static ApiGenericResponse post(String url, String jsonBody) {
        MediaType contentType = MediaType.get("application/json");
        RequestBody body = RequestBody.create(jsonBody, contentType);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        return execute(url, request);
    }

    public static ApiGenericResponse get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return execute(url, request);
    }

    private static ApiGenericResponse execute(String url, Request request) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture<ApiGenericResponse> callback = executor.submit(() -> {
                Call call = CLIENT.newCall(request);
                var apiGenericResponse = new ApiGenericResponse();
                try (Response response = call.execute()) {
                    apiGenericResponse.setSuccess(response.isSuccessful());
                    apiGenericResponse.setStatus(response.code());
                    String responseBody = readBodyAsString(response);
                    apiGenericResponse.setResponseBody(responseBody);
                } catch (IOException e) {
                    System.err.printf("Error to request %s error %s %n", url, e.getMessage());
                }
                return CompletableFuture.completedFuture(apiGenericResponse);
            }).get();

            return callback.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static String readBodyAsString(Response response) {
        try {
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
