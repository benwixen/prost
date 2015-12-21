package com.tripfinger.prost;

import com.tripfinger.prost.annotations.RestMethod;
import com.tripfinger.prost.annotations.UrlParam;
import com.tripfinger.prost.utils.StreamUtils;
import com.tripfinger.prost.utils.Tuple;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RequestHandler extends HttpServlet {

  static Map<String, Tuple<MethodEntry, Map>> restHandlers = new HashMap<>();

  public static void setRestHandler(Class restHandler) {

    for(Method m: restHandler.getDeclaredMethods()) {
      if (m.isAnnotationPresent(RestMethod.class)) {
        RestMethod annotation = m.getAnnotation(RestMethod.class);
        String url = annotation.value();
        MethodEntry methodEntry = new MethodEntry();
        methodEntry.methods.put(annotation.method(), m);
        methodEntry.urlParams = getUrlParamsForMethod(m);
        compileUrlParts(getUrlParts(url), restHandlers, methodEntry);
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String url = req.getPathInfo();
    HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
    Map<String, String[]> parameterMap = req.getParameterMap();
    Map<String, String> parameters = new HashMap<>();
    for (Map.Entry<String, String[]> parameter : parameterMap.entrySet()) {
      parameters.put(parameter.getKey(), parameter.getValue()[0]);
    }
    writeResponse(resp, handleRequest(url, httpMethod, null, parameters, null));
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    setCorsHeaders(resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String body = null;
    Map<String, byte[]> items = new HashMap<>();
    if (ServletFileUpload.isMultipartContent(req)) {

      try {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(100_000_000);
        ServletFileUpload upload = new ServletFileUpload(factory);
        FileItemIterator iterator = upload.getItemIterator(req);
        while (iterator.hasNext()) {
          FileItemStream item = iterator.next();
          String name = item.getFieldName();

          if (!item.isFormField()) {
            items.put(name, StreamUtils.readBytesFromInputStream(item.openStream()));
          }
        }
      }
      catch (FileUploadException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      body = getPostBodyForRequest(req);
    }


    String url = req.getPathInfo();
    HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
    Map<String, String> parameters = new HashMap<>();
    writeResponse(resp, handleRequest(url, httpMethod, body, parameters, items));
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }

  private void setCorsHeaders(HttpServletResponse resp) {
    resp.setHeader("Access-Control-Allow-Origin", "*");
    resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
    resp.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Authorization");
  }

  private void writeResponse(HttpServletResponse resp, HttpResponse response) throws IOException {
    resp.setContentType(response.contentType);
    resp.setStatus(response.status);
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    setCorsHeaders(resp);

    resp.getWriter().println(response.body);
  }

  public HttpResponse handleRequest(String url, HttpMethod httpMethod, String requestBody, Map<String, String> parameters,
                                    Map<String, byte[]> files) {
    List<String> urlParts = getUrlParts(url);

    if (urlParts.size() >= 1) {

      return handleRequest(httpMethod, urlParts, parameters, requestBody, files);
    }
    else {
      return new HttpResponse(404, "URL not valid: " + url);
    }
  }

  public static String getPostBodyForRequest(HttpServletRequest req) {
    try (InputStream input = req.getInputStream()) {
      return StreamUtils.readStringFromInputStream(input);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<String> getUrlParts(String url) {
    return Arrays.asList(url.replaceFirst("^/", "").split("/"));
  }


  private static class MethodEntry {
    public Map<HttpMethod, Method> methods = new HashMap<>();
    public List<String> urlParams = new LinkedList<>();
  }

  private static void compileUrlParts(List<String> urlParts, Map<String, Tuple<MethodEntry, Map>> handlers, MethodEntry m) {
    String urlPart = urlParts.get(0);
    if (urlPart.startsWith(":")) {
      urlPart = "param";
    }
    Tuple<MethodEntry, Map> entry = handlers.get(urlPart);
    if (entry == null) {
      entry = new Tuple<>(null, null);
      handlers.put(urlPart, entry);
    }
    if (urlParts.size() == 1) {
      if (entry.x != null) {
        for (Map.Entry<HttpMethod, Method> method : m.methods.entrySet()) {
          entry.x.methods.put(method.getKey(), method.getValue());
        }
        entry.x.urlParams = m.urlParams;
      }
      else {
        entry.x = m;
      }
    }
    else {
      if (entry.y == null) {
        entry.y = new HashMap<Method, Tuple<Method, Map>>();
      }
      compileUrlParts(urlParts.subList(1, urlParts.size()), entry.y, m);
    }
  }

  private static List<String> getUrlParamsForMethod(Method m) {
    List<String> urlParams = new LinkedList<>();
    Annotation[][] annotationArrays = m.getParameterAnnotations();
    for (Annotation[] parameterAnnotations : annotationArrays) {
      if (parameterAnnotations.length > 0) {
        UrlParam urlParam = (UrlParam)parameterAnnotations[0];
        urlParams.add(urlParam.value());
      }
    }
    return urlParams;
  }

  private static MethodEntry getUrlMethod(List<String> pathElements, List<Object> restParameters,
                                          Map<String, Tuple<MethodEntry, Map>> handlers,
                                          HttpMethod method) {
    String pathElement = pathElements.get(0);
    Tuple<MethodEntry, Map> element = handlers.get(pathElement);
    boolean addParameter = false;
    if (element == null) {
      addParameter = true;
      element = handlers.get("param");
      if (element == null) {
        return null;
      }
    }
    if (pathElements.size() == 1) {
      if (element.x == null) {
        addParameter = true;
        element = handlers.get("param");
      }
      if (!element.x.methods.containsKey(method)) {
        return null;
      }
      if (addParameter) {
        restParameters.add(pathElement);
      }
      return element.x;
    }
    else {
      if (element.y == null) {
        return null;
      }
      if (addParameter) {
        restParameters.add(pathElement);
      }
      MethodEntry entry = getUrlMethod(pathElements.subList(1, pathElements.size()), restParameters, element.y, method);
      if (entry == null && addParameter) {
        restParameters.remove(restParameters.size() - 1);
      }
      if (entry == null && !pathElement.equals("param")) {
        restParameters.add(pathElement);
        List<String> newPathElements  = new LinkedList<>();
        newPathElements.add("param");
        newPathElements.addAll(pathElements.subList(1, pathElements.size()));
        entry = getUrlMethod(newPathElements, restParameters, handlers, method);
        if (entry == null) {
          restParameters.remove(restParameters.size() - 1);
        }
      }
      return entry;
    }
  }

  public static HttpResponse handleRequest(List<String> pathElements, Map<String, String> parameters) {
    return handleRequest(HttpMethod.GET, pathElements, parameters, null, null);
  }

  public static HttpResponse handleRequest(List<String> pathElements, String body) {
    return handleRequest(HttpMethod.POST, pathElements, null, body, null);
  }

  public static HttpResponse handleRequest(HttpMethod httpMethod, List<String> pathElements, Map<String, String> parameters, String body, Map<String, byte[]> files) {

    List<Object> restParameters = new LinkedList<>();
    MethodEntry m = getUrlMethod(pathElements, restParameters, restHandlers, httpMethod);
    if (m == null || !m.methods.containsKey(httpMethod)) {
      return new HttpResponse(404, "Resource not found: " + pathElements);
    }

    try {
      if (httpMethod != HttpMethod.POST) {
        for (String urlParameter : m.urlParams) {
          restParameters.add(parameters.get(urlParameter));
        }
      }
      else {
        restParameters.add(body);

        if (files != null && files.size() > 0) {
          restParameters.add(files);
        }
      }
      HttpResponse response = (HttpResponse)m.methods.get(httpMethod).invoke(null, restParameters.toArray());
      if (response == null) {
        return new HttpResponse(404, "Resource not found: " + pathElements);
      }
      return response;
    }
    catch (Exception e) {
      Throwable inner = e;
      if (InvocationTargetException.class.isInstance(e)) {
        inner = e.getCause();
      }
      StringWriter errors = new StringWriter();
      inner.printStackTrace(new PrintWriter(errors));
      System.out.println(errors.toString());
      return new HttpResponse(500, inner.toString());
    }
  }
}