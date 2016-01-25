# prost

A featherlight REST-framework for Java Servlets. Perfect for bridging server side code with an AngularJS client. Runs well on Google App Engine.

## Configuration

We publish releases to the central maven repository. Add a dependency to your pom.xml:

    <dependency>
      <groupId>com.tripfinger.commons</groupId>
      <artifactId>prost</artifactId>
      <version>0.2</version>
    </dependency>

## Usage

Create a normal class, and use @RestMethod-annotations to expose your API.

    public class RestHandler {

      @RestMethod("/hello")
      public static HttpResponse hello() {
        return new HttpResponse(200, "Hello, World!");
      }
    }
    
Register your class with the prost RequestHandler, f.ex. through a context-listener in web.xml:

    <listener>
      <listener-class>MyConfigurationListener</listener-class>
    </listener>

Example of context-listener:

    public class ConfigurationListener implements ServletContextListener {
    
      protected RequestHandler requestHandler = new RequestHandler();
    
      @Override
      public void contextInitialized(ServletContextEvent event) {
        requestHandler.setRestHandler(MyRestHandlers.class);
      }
      
      @Override
      public void contextDestroyed(ServletContextEvent servletContextEvent) {}
    }

Finally, map requests to the prosts RequestHandler in your web.xml:

    <servlet>
      <servlet-name>requestHandler</servlet-name>
      <servlet-class>com.tripfinger.commons.prost.RequestHandler</servlet-class>
    </servlet>
    <servlet-mapping>
      <servlet-name>requestHandler</servlet-name>
      <url-pattern>/*</url-pattern>
    </servlet-mapping>

