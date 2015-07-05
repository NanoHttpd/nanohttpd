## NanoHTTPD – a tiny web server in Java

*NanoHTTPD* is a light-weight HTTP server designed for embedding in other applications, released under a Modified BSD licence.

It is being developed at Github and uses Apache Maven for builds & unit testing:

 * Build status: [![Build Status](https://api.travis-ci.org/NanoHttpd/nanohttpd.png)](https://travis-ci.org/NanoHttpd/nanohttpd)
 * Coverage Status: [![Coverage Status](https://coveralls.io/repos/NanoHttpd/nanohttpd/badge.svg)](https://coveralls.io/r/NanoHttpd/nanohttpd)
 * Current central released version: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.nanohttpd/nanohttpd/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.nanohttpd/nanohttpd)

## Quickstart

We'll create a custom HTTP server project using Maven for build/dep system. This tutorial assumes you are using a Unix variant and a shell. First, install Maven and Java SDK if not already installed. Then run:

    mvn archetype:generate -DgroupId=com.example -DartifactId=myHellopApp -DinteractiveMode=false
    cd myHellopApp
    
Edit `pom.xml`, and add this between \<dependencies\>:
 
	<dependency>
		<groupId>org.nanohttpd</groupId>
		<artifactId>nanohttpd</artifactId>
		<version>2.2.0-SNAPSHOT</version>
	</dependency>

Edit `src/main/java/com/example/App.java` and replace it with:

	package com.example;

	import java.util.Map;
	import java.io.IOException;
	import fi.iki.elonen.NanoHTTPD;

	public class App extends NanoHTTPD {

	    public App() throws IOException {
	        super(8080);
	        start();
			System.out.println( "\nRunning! Point your browers to http://localhost:8080/ \n" );
	    }

	    public static void main(String[] args) {
			try {
			    new App();
			}
			catch( IOException ioe ) {
				System.err.println( "Couldn't start server:\n" + ioe );
			}
	    }

	    @Override
	    public Response serve(IHTTPSession session) {
	        String msg = "<html><body><h1>Hello server</h1>\n";
	        Map<String, String> parms = session.getParms();
	        if (parms.get("username") == null) {
	            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
	        } else {
	            msg += "<p>Hello, " + parms.get("username") + "!</p>";
	        }
	        return newFixedLengthResponse( msg + "</body></html>\n" );
	    }
	}
 

Compile and run the server:
 
    mvn compile
    mvn exec:java -Dexec.mainClass="com.example.App"
    
If it started ok, point your browser at <http://localhost:8080/> and enjoy a web server that asks your name and replies with a greeting. 

## Status

We are currently in the process of stabilizing NanoHttpd from the many pull requests and feature requests that were integrated over the last few months. The next release will come soon, and there will not be any more "intended" major changes before the next release. If you want to use the bleeding edge version, you can clone it from Github, or get it from sonatype.org (see "Maven dependencies / Living on the edge" below).

## Project structure

NanoHTTPD project currently consist of four parts:

 * `/core` – Fully functional HTTP(s) server consisting of one (1) Java file, ready to be customized/inherited for your own project

 * `/samples` – Simple examples on how to customize NanoHTTPD. See *HelloServer.java* for a killer app that greets you enthusiastically!

 * `/websocket` – Websocket implementation, also in a single Java file. Depends on core.

 * `/webserver` – Standalone file server. Run & enjoy. A popular use seems to be serving files out off an Android device.

## Features
### Core
* Only one Java file, providing HTTP 1.1 support.
* No fixed config files, logging, authorization etc. (Implement by yourself if you need them. Errors are passed to java.util.logging, though.)
* Support for HTTPS (SSL)
* Basic support for cookies
* Supports parameter parsing of GET and POST methods.
* Some built-in support for HEAD, POST and DELETE requests. You can easily implement/customize any HTTP method, though.
* Supports file upload. Uses memory for small uploads, temp files for large ones.
* Never caches anything.
* Does not limit bandwidth, request time or simultaneous connections by default.
* All header names are converted to lower case so they don't vary between browsers/clients.
* Persistent connections (Connection "keep-alive") support allowing multiple requests to be served over a single socket connection.

### Websocket
* Tested on Firefox, Chrome and IE.

### Webserver
* Default code serves files and shows (prints on console) all HTTP parameters and headers.
* Supports both dynamic content and file serving.
* File server supports directory listing, `index.html` and `index.htm`.
* File server supports partial content (streaming & continue download).
* File server supports ETags.
* File server does the 301 redirection trick for directories without `/`.
* File server serves also very long files without memory overhead.
* Contains a built-in list of most common MIME types.
* Runtime extension support (extensions that serve particular MIME types) - example extension that serves Markdown formatted files. Simply including an extension JAR in the webserver classpath is enough for the extension to be loaded.

## Maven dependencies

NanoHTTPD is a Maven based project and deployed to central. Most development environments have means to access the central repository. The coordinates to use in Maven are: 

	<dependencies>
		<dependency>
			<groupId>org.nanohttpd</groupId>
			<artifactId>nanohttpd</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>
	</dependencies>

(Replace `CURRENT_VERSION` with whatever is reported latest at <http://nanohttpd.org/>.)

The coordinates for your development environment should correspond to these. When looking for an older version take care because we switched groupId from *com.nanohttpd* to *org.nanohttpd* in mid 2015.

Next it depends what you are useing nanohttpd for, there are tree main usages.

### Develop your own specialized HTTP service

For a specialized HTTP (HTTPS) service you can use the module with artifactId *nanohttpd*.

		<dependency>
			<groupId>org.nanohttpd</groupId>
			<artifactId>nanohttpd</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>
		
Here you write your own subclass of *fi.iki.elonen.NanoHTTPD* to configure and to serve the requests.
  
### Develop a websocket based service    

For a specialized websocket service you can use the module with artifactId *nanohttpd-websocket*.

		<dependency>
			<groupId>org.nanohttpd</groupId>
			<artifactId>nanohttpd-websocket</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>

Here you write your own subclass of *fi.iki.elonen.NanoWebSocketServer* to configure and to serve the websocket requests. A small standard echo example is included as *fi.iki.elonen.samples.echo.DebugWebSocketServer*. You can use it as a starting point to implement your own services.

### Develop a custom HTTP file server    

For a more classic aproach, perhaps to just create a HTTP server serving mostly service files from your disk, you can use the module with artifactId *nanohttpd-webserver*.

		<dependency>
			<groupId>org.nanohttpd</groupId>
			<artifactId>nanohttpd-webserver</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>

The included class *fi.iki.elonen.SimpleWebServer* is intended to be used as a starting point for your own implementation but it also can be used as is. Staring the class as is will start a http server on port 8080 and publishing the current directory.  

### Living on the edge

The latest Github master version can be fetched through sonatype.org:

	<dependencies>
		<dependency>
			<artifactId>nanohttpd</artifactId>
			<groupId>org.nanohttpd</groupId>
			<version>XXXXX-SNAPSHOT</version>
		</dependency>
	</dependencies>
	...
	<repositories>
		<repository>
			<id>sonatype-snapshots</id>
			<url>https://oss.sonatype.org/content/repositories/snapshots</url>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
		</repository>
	</repositories>


-----

*Thank you to everyone who has reported bugs and suggested fixes.*
