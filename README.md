## What is "nanohttpd"?

*NanoHttpd* is a light-weight HTTP server designed for embedding in other applications.

*NanoHttpd* has been released under a Modified BSD licence.

## Current major development efforts

Waffle.io Issue Tracking: [![Stories in Ready](https://badge.waffle.io/NanoHttpd/nanohttpd.png?label=ready)](https://waffle.io/NanoHttpd/nanohttpd)  

*Core*
* Please take a look at the new "ssl-support" branch containing submitted code adding SSL support to NanoHttpd.  It's a great new feature that needs all eyes to polish in preparation for a release, making sure it works on all platforms.

*Webserver*
* Internal architecture support URL rewriting.  Serving "index.*" files now utilizes the feature.  Capability needs to be extended to read and apply rewrite rules at runtime.
* Plugin support - plugins that transform the source (eg PHP, Markdown) can now support caching generate files.  Add caching to the markdown plugin as an example.

## Core Features
* Only one Java file, providing HTTP 1.1 support.
* 2 "flavors" - one at "current" standards and one strictly Java 1.1 compatible.
* Released as open source, free software, under a Modified BSD licence.
* No fixed config files, logging, authorization etc. (Implement by yourself if you need them.)
* Experimental support for SSL (see the 'ssl-support' branch in git)
* Basic support for cookies
* Supports parameter parsing of GET and POST methods.
* Rudimentary PUT support (added in 1.25).
* Support for HEAD and DELETE requests.
* Supports single and multi-value parameters (w/ a helper method) if needed.
* Supports file upload (since version 1.2, 2010) with minimal memory overhead.
* Never caches anything.
* Doesn't limit bandwidth, request time or simultaneous connections.
* All header names are converted lowercase so they don't vary between browsers/clients.
* Very low memory overhead when processing even the largest of requests.
* Temp file usage and threading model are easily cutomized.
* Persistent connections (Connection "keep-alive") support allowing multiple requests to be served over a single socket connection.

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
* Runtime extension support (extensions that serve particular mime types) - example extension that serves Markdown formatted files. Simply including an extension JAR in the webserver classpath is enough for the extension to be loaded.

## How is the project managed?

The project is managed with a "fork and pull-request" pattern.

If you want to contribute, fork this repo and submit a pull-request of your changes when you're ready.

Anyone can create Issues, and pull requests should be tied back to an issue describing the purpose of the submitted code.

## The Tests

In an ideal world for a bug fix: write a test of your own that fails as a result of the bug being present.  Then fix the bug so that your unit-test passes.  The test will now be a guard against the bug ever coming back.

Similarly for enhancements, exercise your code with tests.

Whatever else happens, if you make changes to the code, the unit & integration test suite (under ```core/src/main/test/```) should all continue to function.  Pull requests with broken tests will be rejected.

## Where can I find the original (Java1.1) NanoHttpd?

The original (Java 1.1 project) and the Java 6 project merged in early 2013 to pool resources
around "NanoHttpd" as a whole, regardless of flavor.  Development of the Java 1.1 version continues
as a permanent branch ("nanohttpd-for-java1.1") in the main http://github.com/NanoHttpd/nanohttpd repository.

## How do I use nanohttpd?

Firstly take a look at the "samples" sub-module.  The sample code illustrates using NanoHttpd in various ways.

Secondly, you can run the standalone *NanoHttpd Webserver*.

Or, create your own class that extends `NanoHTTPD` and overrides one of the two flavors of the `serve()` method.  For example:

```java
package fi.iki.elonen.debug;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugServer extends NanoHTTPD {
    public DebugServer() {
        super(8080);
    }

    public static void main(String[] args) {
        ServerRunner.run(DebugServer.class);
    }

    @Override public Response serve(IHTTPSession session) {
        Map<String, List<String>> decodedQueryParameters =
            decodeParameters(session.getQueryParameterString());

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head><title>Debug Server</title></head>");
        sb.append("<body>");
        sb.append("<h1>Debug Server</h1>");

        sb.append("<p><blockquote><b>URI</b> = ").append(
            String.valueOf(session.getUri())).append("<br />");

        sb.append("<b>Method</b> = ").append(
            String.valueOf(session.getMethod())).append("</blockquote></p>");

        sb.append("<h3>Headers</h3><p><blockquote>").
            append(toString(session.getHeaders())).append("</blockquote></p>");

        sb.append("<h3>Parms</h3><p><blockquote>").
            append(toString(session.getParms())).append("</blockquote></p>");

        sb.append("<h3>Parms (multi values?)</h3><p><blockquote>").
            append(toString(decodedQueryParameters)).append("</blockquote></p>");

        try {
            Map<String, String> files = new HashMap<String, String>();
            session.parseBody(files);
            sb.append("<h3>Files</h3><p><blockquote>").
                append(toString(files)).append("</blockquote></p>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        sb.append("</body>");
        sb.append("</html>");
        return new Response(sb.toString());
    }

    private String toString(Map<String, ? extends Object> map) {
        if (map.size() == 0) {
            return "";
        }
        return unsortedList(map);
    }

    private String unsortedList(Map<String, ? extends Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (Map.Entry entry : map.entrySet()) {
            listItem(sb, entry);
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private void listItem(StringBuilder sb, Map.Entry entry) {
        sb.append("<li><code><b>").append(entry.getKey()).
            append("</b> = ").append(entry.getValue()).append("</code></li>");
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

http://nanohttpd.com - went live July 1st, 2013.

## Version History (Java 6+ version)
* 2.0.5 (2013-12-12) : Cleanup and stability fixes.
* 2.0.4 (2013-09-15) : Added basic cookie support, experimental SSL support and runtime extensions.
* 2.0.3 (2013-06-17) : Implemented 'Connection: keep-alive', (tested against latest Mozilla Firefox).
* 2.0.2 (2013-06-06) : Polish for the webserver, and fixed a bug causing stack-traces on Samsung Phones.
* 2.0.1 (2013-05-27) : Non-English UTF-8 decoding support for URLS/Filenames
* 2.0.0 (2013-05-21) : Released - announced on FreeCode.com
* (2013-05-20) : Test suite looks complete.
* (2013-05-05) : Web server pulled out of samples and promoted to top-level project
* (2013-03-09) : Work on test suite begins - the push for release 2.0.0 begins!
* (2013-01-04) : Initial commit on "uplift" fork

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
