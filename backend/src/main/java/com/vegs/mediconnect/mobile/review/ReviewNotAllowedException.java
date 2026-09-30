package com.vegs.mediconnect.mobile.review;

/** A review the visit cannot take: not happened yet, cancelled, or already reviewed. */
public class ReviewNotAllowedException extends RuntimeException {

    public ReviewNotAllowedException(String message) {
        super(message);
    }
}
