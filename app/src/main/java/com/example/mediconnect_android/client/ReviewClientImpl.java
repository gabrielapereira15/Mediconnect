package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;

public class ReviewClientImpl implements ReviewClient {
    private final String baseurl = "https://mediconnect-latest.onrender.com";

    @Override
    public Boolean createReview(String appointmentId, Double score, String description) {
        String url = baseurl + "/api/mobile/reviews";

        String reviewJson = String.format(
                "{\"appointmentId\": \"%s\", \"score\": %s, \"description\": \"%s\"}",
                appointmentId,
                score != null ? score : 0.0,
                description != null ? description : ""
        );

        ApiGenericResponse response = OkHttpClientHelper.post(url, reviewJson);
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("ReviewClientImpl", "Error creating review: " + response.getResponseBody());
            return false;
        }
    }
}
