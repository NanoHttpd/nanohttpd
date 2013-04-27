## What is "nanohttpd"?

*NanoHttpd* is a light-weight HTTP server designed for embedding in other applications.

*NanoHttpd* has been released under a Modified BSD licence.

## Core Features
* Only one Java file.
* 2 "flavors" - one strictly Java 1.1 compatible, one at "current" standards.
* Released as open source, free software, under a Modified BSD licence.
* No fixed config files, logging, authorization etc. (Implement by yourself if you need them.)
* Supports parameter parsing of GET and POST methods  (+ rudimentary PUT support in 1.25)
* Parameter names must be unique, with a helper method to extract multi-value parameters if needed.
* Supports file upload (since version 1.2, 2010)
* Never caches anything.
* Doesn't limit bandwidth, request time or simultaneous connections.
* All header names are converted lowercase so they don't vary between browsers/clients.
* Very low memory overhead when processing even the largest of requests.
* Temp file usage and threading model are easily cutomized

## Webserver Features
* Supports both dynamic content and file serving.
* Default code serves files and shows all HTTP parameters and headers.
* File server supports directory listing, ```index.html``` and ```index.htm```.
* File server supports partial content (streaming).
* File server supports ETags.
* File server does the 301 redirection trick for directories without ```/```.
* File server supports simple skipping for files (continue download).
* File server serves also very long files without memory overhead.
* Contains a built-in list of most common mime types.

## How is the project managed?

The project is managed with a "fork and pull-request" pattern.  If you want to contribute, fork this repo and submit a pull-request of your changes when you're ready.  Anyone can create Issues, and pull requests should be tied back to an issue describing the purpose of the submitted code.

## Where can I find the original (Java1.1) NanoHttpd?

The original (Java 1.1 project) and the Java 6 project merged in early 2013 to pool resources 
around "NanoHttpd" as a whole, regardless of flavor.  Development of the Java 1.1 version continues 
as a permanent branch ("nanohttpd-for-java1.1") in the main http://github.com/NanoHttpd/nanohttpd repository.

## How do I use nanohttpd?

Firstly take a look at the "samples" sub-module.  The sample code illustrates using NanoHttpd in various ways.

Secondly, you can run the standalone *NanoHttpd Webserver*.

Or, create your own class that extends `NanoHTTPD` and overrides the `serve()` method.  For example:

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

The Java 6 version of *nanohttpd* was born when we realized that embedding Jetty inside our 
Android application was going to inflate the size without bringing along features that we 
were going to need.  The search for a smaller more streamlined HTTP server lead us 
to *nanohttpd* as the project had started with exactly the same goals, but we wanted to 
clear up the old code - move from Java 1.1, run _static code analysis_ tools and cleanup 
the findings and pull out sample/test code from the source.

In the words of the original founder of the project
> I couldn't find a small enough, embeddable and easily modifiable HTTP server
> that I could just copy and paste into my other Java projects. Every one of them
> consisted of dozens of .java files and/or jars, usually with - from my point
> of view - "overkill features" like servlet support, web administration,
> configuration files, logging etc.

Since that time we fixed a number of bugs, moved the build to _maven_ and pulled out 
the samples from the runtime JAR to further slim it down.

The two projects pooled resources in early 2013, merging code-bases, to better support the
user base and reduce confusion over why _two_ NanoHttpd projects existed.

## Version History (Java 1.1 version)

* 1.27 (2013-04-01): Merged several bug fixes from github forks
* 1.26 (2013-03-27): fixed an off-by one bug
* 1.25 (2012-02-12): rudimetary PUT support, buffer size now configurable, support for etag "if-none-match" check, log output stream now configurable
* 1.24 (2011-08-04): etags + video mime types (for HTML5 video streaming)
* 1.23 (2011-08-02): better support for partial requests
* 1.22 (2011-07-21): support for custom www root dir
* 1.21 (2011-01-03): minor bug fixes
* 1.2  (2010-12-31): file upload (by Konstantinos Togias) and some small bug fixes
* 1.14 (2010-08-20): added a stop() function
* 1.13 (2010-06-27): fixed a wrong case in 'range' header
* 1.12 (2010-01-07): fixed a null ptr exception
* 1.11 (2008-04-21): fixed a double URI decoding (caused problems when there was a percent-coded percent)
* 1.10 (2007-02-09): improved browser compatibility by forcing headers lowercase; fixed a POST method over-read bug
* 1.05 (2006-03-30): honor Content-Length header; support for clients that leave TCP connection open; better MIME support for symlinked files
* 1.02 (2005-07-08): fixed a stream read starvation bug
* 1.01 (2003-04-03): first published version

Thank you to everyone who has reported bugs and suggested fixes.
