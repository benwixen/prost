package com.tripfinger.prost;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RequestHandlerTest {

  @Test
  public void testRequestHandler() {

    RequestHandler.setRestHandler(RestHandler.class);

    Map<String, String> paramaters = new HashMap<>();
    List<String> pathElements = Arrays.asList("apple");
    HttpResponse response = RequestHandler.handleRequest(pathElements, paramaters);
    assertEquals(200, response.status);
    assertEquals(String.format("{\"status\": %d, \"message\": \"%s\", \"id\": null}", 200, "Apple"), response.body);

    pathElements = Arrays.asList("hello", "Boy");
    response = RequestHandler.handleRequest(pathElements, paramaters);
    assertEquals(200, response.status);
    assertEquals("Hello, Boy", response.body);

    pathElements = Arrays.asList("bye", "Mama");
    response = RequestHandler.handleRequest(pathElements, "3");
    assertEquals(200, response.status);
    assertEquals("Bye, bye, bye, Mama", response.body);

  }
}
