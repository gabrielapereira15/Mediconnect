package com.example.mediconnect_android.client;

public interface ReviewClient {
    Boolean createReview(String appointmentId, Double score, String description);
}
