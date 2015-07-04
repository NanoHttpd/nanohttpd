## NanoHTTPD – a tiny web server in Java

*NanoHTTPD* is a light-weight HTTP server designed for embedding in other applications, released under a Modified BSD licence.

It is being developed at Github and uses Apache Maven for builds & unit testing:

 * Build status: [![Build Status](https://api.travis-ci.org/NanoHttpd/nanohttpd.png)](https://travis-ci.org/NanoHttpd/nanohttpd)
 * Coverage Status: [![Coverage Status](https://coveralls.io/repos/NanoHttpd/nanohttpd/badge.svg)](https://coveralls.io/r/NanoHttpd/nanohttpd)
 * Current central released version: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.nanohttpd/nanohttpd/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.nanohttpd/nanohttpd)

## Getting started

*(TODO: list of commands to fetch from Maven, build and run Webserver and/or HelloServer here?)*

## Status

We are currently in the process of stabilizing NanoHttpd from the many pull requests and feature requests that were integrated over the last few months. The next release will come soon, and there will not be any more "intended" major changes before the next release. If you want to use the bleeding edge version, you can clone it from Github, or get it from sonatype.org:

	<dependencies>
		<dependency>
			<artifactId>nanohttpd</artifactId>
			<groupId>org.nanohttpd</groupId>
			<version>2.2.0-SNAPSHOT</version>
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

The other artifacts are also available there.

The project currently consist of four parts:

 * `/core` – Fully functional HTTP(s) server consisting of one (1) Java file, ready to be customized/inherited for your own project

 * `/samples` – Simple examples on how to customize NanoHTTPD. See *HelloServer.java* for a killer app that greets you enthusiastically!

 * `/websocket` – Websocket implementation, also in a single Java file. Depends on core.

 * `/webserver` – Standalone file server. Run & enjoy. A popular use seems to be serving files out off an Android device.

## Features of Core
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

## Websocket Support
* Tested on Firefox, Chrome and IE.

## Webserver Features
* Default code serves files and shows (prints on console) all HTTP parameters and headers.
* Supports both dynamic content and file serving.
* File server supports directory listing, `index.html` and `index.htm`.
* File server supports partial content (streaming & continue download).
* File server supports ETags.
* File server does the 301 redirection trick for directories without `/`.
* File server serves also very long files without memory overhead.
* Contains a built-in list of most common MIME types.
* Runtime extension support (extensions that serve particular MIME types) - example extension that serves Markdown formatted files. Simply including an extension JAR in the webserver classpath is enough for the extension to be loaded.

*Thank you to everyone who has reported bugs and suggested fixes.*
