package com.tripfinger.prost;

public enum HttpMethod {
  GET("GET"),
  POST("POST"),
  DELETE("DELETE");

  public final String value;
  HttpMethod(final String newValue) {
    value = newValue;
  }
}
