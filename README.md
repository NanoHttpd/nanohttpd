## What is "nanohttpd"?

*NanoHttpd* is a light-weight HTTP server designed for embedding in other applications.

*NanoHttpd* is a fork of the original tiny, easily embeddable HTTP server in Java.  Whereas the original 
was written and maintained at the JDK 1.1 level, this fork aims to bring the code up to JDK 6
(Generics, Map/List vs Hashtable/Vector, Iterator vs Enumerator, etc).

*NanoHttpd* has been released under a Modified BSD licence.

## How is the project managed?

The project is managed with a "fork and pull-request" pattern.  If you want to contribute, fork this repo and submit a pull-request of your changes when you're ready.  Anyone can create Issues, and pull requests should be tied back to an issue describing the purpose of the submitted code.

## Where can I find the original NanoHttpd?

The original Project website is at: http://iki.fi/elonen/code/nanohttpd/

## How do I use nanohttpd?

Create your own class that extends `NanoHTTPD` and overrides the `serve()` method.  For example:

```java
public class DebugServer extends NanoHTTPD {
    /**
     * Constructs an HTTP server on given port.
     */
    public DebugServer() {
        super(8080);
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head><title>Debug Server</title></head>");
        sb.append("<body>");
        sb.append("<h1>Response</h1>");
        sb.append("<p><blockquote><b>URI -</b> ").append(uri).append("<br />");
        sb.append("<b>Method -</b> ").append(method).append("</blockquote></p>");
        sb.append("<h3>Headers</h3><p><blockquote>").append(header).append("</blockquote></p>");
        sb.append("<h3>Parms</h3><p><blockquote>").append(parms).append("</blockquote></p>");
        sb.append("<h3>Files</h3><p><blockquote>").append(files).append("</blockquote></p>");
        sb.append("</body>");
        sb.append("</html>");
        return new Response(sb.toString());
    }

    public static void main(String[] args) {
        ServerRunner.run(DebugServer.class);
    }
}
```

## Why fork the original repo?

This version of *nanohttpd* was born when we realized that embedding Jetty inside our Android application was going to inflate the size without bringing along features that we were going to need.  The search for a smaller more streamlined HTTP server lead us to *nanohttpd* but we wanted to clear up the old code - move from Java 1.1, run _static code analysis_ tools and cleanup the findings and pull out sample/test code from the source.

Since that start we fixed a number of bugs and moved the build to _maven_ and pulled out the samples from the runtime JAR to further slim it down.
