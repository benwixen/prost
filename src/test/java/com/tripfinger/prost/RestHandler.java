package com.tripfinger.prost;

import com.tripfinger.prost.annotations.RestMethod;

public class RestHandler {

  @RestMethod("/apple")
  public static HttpResponse getFruits() {
    return new HttpResponse(200, "Apple");
  }

  @RestMethod("/hello/:name")
  public static HttpResponse sayHello(String name) {
    HttpResponse response = new HttpResponse();
    response.body = "Hello, " + name;
    return response;
  }

  @RestMethod(method = HttpMethod.POST, value = "/bye/:name")
  public static HttpResponse sayBye(String name, String body) {
    HttpResponse response = new HttpResponse();
    Integer times = Integer.parseInt(body);
    response.body = "Bye, ";
    for (int i = 1; i < times; i++) {
      response.body += "bye, ";
    }
    response.body = response.body + name;
    return response;
  }

}
