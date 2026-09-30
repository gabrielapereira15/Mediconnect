package com.example.mediconnect_android.client;

import android.util.Log;

import com.example.mediconnect_android.client.response.ApiGenericResponse;
import com.google.gson.JsonObject;

public class ReviewClientImpl implements ReviewClient {

    /**
     * Sends a review of one of the patient's own visits.
     *
     * The body is built as JSON rather than pasted into a template: the
     * description is whatever the patient typed, and a quote or a line break
     * in it used to break the request, or let the text add fields of its own.
     */
    @Override
    public Boolean createReview(String appointmentId, Double score, String description) {
        String url = ApiConfig.url("/api/mobile/reviews");

        JsonObject review = new JsonObject();
        review.addProperty("appointmentId", appointmentId);
        review.addProperty("score", score != null ? score : 0.0);
        review.addProperty("description", description != null ? description : "");

        ApiGenericResponse response = OkHttpClientHelper.post(url, review.toString());
        if (response.isSuccess()) {
            return true;
        } else {
            Log.e("ReviewClientImpl", "Error creating review: " + response.getResponseBody());
            return false;
        }
    }
}
