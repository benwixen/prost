package com.tripfinger.prost;

import com.tripfinger.prost.annotations.Guard;
import com.tripfinger.prost.annotations.Open;
import com.tripfinger.prost.annotations.RestMethod;
import com.tripfinger.prost.model.HttpResponse;

@Guard
public class GuardedRestHandler {

  @Open
  @RestMethod("/apple")
  public static HttpResponse getFruits() {
    return new HttpResponse(200, "Apple", null);
  }

  @RestMethod("/hello/:name")
  public static HttpResponse sayHello(String name) {
    HttpResponse response = new HttpResponse();
    response.body = "Hello, " + name;
    return response;
  }
}
