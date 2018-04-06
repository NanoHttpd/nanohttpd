## NanoHTTPD – a tiny web server in Java

*NanoHTTPD* is a light-weight HTTP server designed for embedding in other applications, released under a Modified BSD licence.

It is being developed at Github and uses Apache Maven for builds & unit testing:

 * Build status: [![Build Status](https://api.travis-ci.org/NanoHttpd/nanohttpd.png)](https://travis-ci.org/NanoHttpd/nanohttpd)
 * Coverage Status: [![Coverage Status](https://coveralls.io/repos/NanoHttpd/nanohttpd/badge.svg)](https://coveralls.io/r/NanoHttpd/nanohttpd)
 * Current central released version: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.nanohttpd/nanohttpd/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.nanohttpd/nanohttpd)

## Quickstart

We'll create a custom HTTP server project using Maven for build/dep system. This tutorial assumes you are using a Unix variant and a shell. First, install Maven and Java SDK if not already installed. Then run:

    mvn compile
    mvn exec:java -pl webserver -Dexec.mainClass="org.nanohttpd.webserver.SimpleWebServer"
    
You should now have a HTTP file server running on <http://localhost:8080/>.

### Custom web app

Let's raise the bar and build a custom web application next:

    mvn archetype:generate -DgroupId=com.example -DartifactId=myHellopApp -DinteractiveMode=false
    cd myHellopApp
    
Edit `pom.xml`, and add this between \<dependencies\>:
 
	<dependency>
		<groupId>org.nanohttpd</groupId> <!-- <groupId>com.nanohttpd</groupId> for 2.1.0 and earlier -->
		<artifactId>nanohttpd</artifactId>
		<version>2.2.0</version>
	</dependency>
	
Edit `src/main/java/com/example/App.java` and replace it with:
```java
    package com.example;
    
    import java.io.IOException;
    import java.util.Map;
    
    import fi.iki.elonen.NanoHTTPD;
    // NOTE: If you're using NanoHTTPD >= 3.0.0 the namespace is different,
    //       instead of the above import use the following:
	// import org.nanohttpd.NanoHTTPD;
    
    public class App extends NanoHTTPD {
    
        public App() throws IOException {
            super(8080);
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
        }
    
        public static void main(String[] args) {
            try {
                new App();
            } catch (IOException ioe) {
                System.err.println("Couldn't start server:\n" + ioe);
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
            return newFixedLengthResponse(msg + "</body></html>\n");
        }
    }
```

Compile and run the server:
 
    mvn compile
    mvn exec:java -Dexec.mainClass="com.example.App"
    
If it started ok, point your browser at <http://localhost:8080/> and enjoy a web server that asks your name and replies with a greeting. 

### Nanolets

Nanolets are like servlets only that they have a extremely low profile. They offer an easy to use system for a more complex server application.
This text has to be extended with an example, so for now take a look at the unit tests for the usage. <https://github.com/NanoHttpd/nanohttpd/blob/master/nanolets/src/test/java/org/nanohttpd/junit/router/AppNanolets.java>

## Status

We are currently in the process of stabilizing NanoHTTPD from the many pull requests and feature requests that were integrated over the last few months. The next release will come soon, and there will not be any more "intended" major changes before the next release. If you want to use the bleeding edge version, you can clone it from Github, or get it from sonatype.org (see "Maven dependencies / Living on the edge" below).

## Project structure

NanoHTTPD project currently consist of four parts:

 * `/core` – Fully functional HTTP(s) server consisting of one (1) Java file, ready to be customized/inherited for your own project.

 * `/samples` – Simple examples on how to customize NanoHTTPD. See *HelloServer.java* for a killer app that greets you enthusiastically!

 * `/websocket` – Websocket implementation, also in a single Java file. Depends on core.

 * `/webserver` – Standalone file server. Run & enjoy. A popular use seems to be serving files out off an Android device.

 * `/nanolets` – Standalone nano app server, giving a servlet like system to the implementor.

 * `/fileupload` – integration of the apache common file upload library.

## Features
### Core
* Only one Java file, providing HTTP 1.1 support.
* No fixed config files, logging, authorization etc. (Implement by yourself if you need them. Errors are passed to java.util.logging, though.)
* Support for HTTPS (SSL).
* Basic support for cookies.
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
* Simple [CORS](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) support via `--cors` parameter
  * by default serves `Access-Control-Allow-Headers: origin,accept,content-type`
  * possibility to set `Access-Control-Allow-Headers` by setting System property: `AccessControlAllowHeader`
  * _example: _ `-DAccessControlAllowHeader=origin,accept,content-type,Authorization`
  * possible values:
      * `--cors`: activates CORS support, `Access-Control-Allow-Origin` will be set to `*`.
      * `--cors=some_value`: `Access-Control-Allow-Origin` will be set to `some_value`. 

**_CORS argument examples_**


* `--cors=http://appOne.company.com`
* `--cors="http://appOne.company.com, http://appTwo.company.com"`: note the double quotes so that the two URLs are considered part of a single argument.

## Maven dependencies

NanoHTTPD is a Maven based project and deployed to central. Most development environments have means to access the central repository. The coordinates to use in Maven are: 

	<dependencies>
		<dependency>
			<groupId>org.nanohttpd</groupId> <!-- <groupId>com.nanohttpd</groupId> for 2.1.0 and earlier -->
			<artifactId>nanohttpd</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>
	</dependencies>

(Replace `CURRENT_VERSION` with whatever is reported latest at <http://nanohttpd.org/>.)

The coordinates for your development environment should correspond to these. When looking for an older version take care because we switched groupId from *com.nanohttpd* to *org.nanohttpd* in mid 2015.

Next it depends what you are using NanoHTTPD for, there are three main usages.

## Gradle dependencies

In gradle you can use NanoHTTPD the same way because gradle accesses the same central repository:

	dependencies {
		runtime(
			[group: 'org.nanohttpd', name: 'nanohttpd', version: 'CURRENT_VERSION'],
		)
	}

(Replace `CURRENT_VERSION` with whatever is reported latest at <http://nanohttpd.org/>.)

Just replace the name with the artifact id of the module you want to use and gradle will find it for you. 

### Develop your own specialized HTTP service

For a specialized HTTP (HTTPS) service you can use the module with artifactId *nanohttpd*.

		<dependency>
			<groupId>org.nanohttpd</groupId> <!-- <groupId>com.nanohttpd</groupId> for 2.1.0 and earlier -->
			<artifactId>nanohttpd</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>
		
Here you write your own subclass of *org.nanohttpd.NanoHTTPD* to configure and to serve the requests.
  
### Develop a websocket based service    

For a specialized websocket service you can use the module with artifactId *nanohttpd-websocket*.

		<dependency>
			<groupId>org.nanohttpd</groupId> <!-- <groupId>com.nanohttpd</groupId> for 2.1.0 and earlier -->
			<artifactId>nanohttpd-websocket</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>

Here you write your own subclass of *org.nanohttpd.NanoWebSocketServer* to configure and to serve the websocket requests. A small standard echo example is included as *org.nanohttpd.samples.echo.DebugWebSocketServer*. You can use it as a starting point to implement your own services.

### Develop a custom HTTP file server    

For a more classic approach, perhaps to just create a HTTP server serving mostly service files from your disk, you can use the module with artifactId *nanohttpd-webserver*.

		<dependency>
			<groupId>org.nanohttpd</groupId>
			<artifactId>nanohttpd-webserver</artifactId>
			<version>CURRENT_VERSION</version>
		</dependency>

The included class *org.nanohttpd.SimpleWebServer* is intended to be used as a starting point for your own implementation but it also can be used as is. Starting the class as is will start a HTTP server on port 8080 and publishing the current directory.

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

### generating an self signed SSL certificate

Just a hint how to generate a certificate for localhost.

	keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048 -ext SAN=DNS:localhost,IP:127.0.0.1  -validity 9999

This will generate a keystore file named 'keystore.jks' with a self signed certificate for a host named localhost with the IP address 127.0.0.1 . Now
you can use:

	server.makeSecure(NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray()), null);

Before you start the server to make NanoHTTPD serve HTTPS connections, when you make sure 'keystore.jks' is in your classpath.
 
-----

*Thank you to everyone who has reported bugs and suggested fixes.*
