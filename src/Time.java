/*
 *
 *  Time.java - simple web server for displaying system time
 *
 *  Time.java is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License v3 as published by the
 *  Free Software Foundation. For more details see the file LICENSE.
 *
 */

import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.io.IOException;
import java.time.Instant;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;


/*
 * ==== code =============================
 */

public class Time {

    static String rootText  = "<html><body><h1>timetraveller</h1>\n<p>For more information, please read <a href='/about'>/about</a>.</p></body></html>\n";
    static String aboutText = "<html><body>\n<h1>timetraveller</h1>\n<p>This is a simple web server that answers with the current time as seconds since 1970 on <a href='/ss1970'>/ss1970</a>. It's intended to be used as test object when shifting kubernetes/openshift containers in time.</p>\n</body></html>\n";

    // main class
    public static void main(String[] args) throws Exception {
        HttpServer timeSrv = HttpServer.create(new InetSocketAddress(8080), 0);

        timeSrv.createContext("/",       new textHandler(rootText)  );
        timeSrv.createContext("/about",  new textHandler(aboutText) );
        timeSrv.createContext("/ss1970", new textHandler("") );
        timeSrv.createContext("/diff",   new diffHandler() );
        timeSrv.createContext("/time",   new timeHandler() );

        timeSrv.setExecutor(null); // creates a default executor
        timeSrv.start();
    }


    /*
     *  handler to send a simple text
     *  - parameter: text to send as reponse
     *  - if text is empty string, then send current time as seconds since UNIX epoch
     */
    static class textHandler implements HttpHandler {

	// save given reponse text in own variable
	private String responseText;

	// define custom handler to be able to receive string parameter
	public  textHandler(String responseText) {
	    this.responseText = responseText;
	}

        @Override
	public void handle(HttpExchange c) throws IOException {

            String finalResponseText = responseText;
	    // if given response is empty, then simply send epoch time
	    if (finalResponseText == "") {
		finalResponseText = Long.toString( java.time.Instant.now().getEpochSecond() ) + " <-\n";
	    }
	    // prepare and send http reponse
	    c.sendResponseHeaders(200, finalResponseText.length());
	    OutputStream rs = c.getResponseBody();
	    rs.write(finalResponseText.getBytes());
	    rs.close();
	}
    }


    /*
     *  handler to print difference to current time in seconds
     *  - takes second path of context as timestamp parameter in seconds since 1970
     *  - e.g. /context/1234567
     */
    static class diffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    // get the context path and split it up; [1]=>top context, [2]=>LongInt parameter
	    String pathCalled   = c.getRequestURI().getPath();
	    String[] pathArray  = pathCalled.split("[/]");
	    String responseText = "";

	    // check if request is valid and if so, convert LongInt and calculate difference to now()
	    if (pathArray.length == 3) {
		if( pathArray[2].matches("\\d*") ){
		    long longGiven = Long.parseLong( pathArray[2] );
		    long since1970 = java.time.Instant.now().getEpochSecond();
        	    responseText = "Difference to now() is " + Long.toString(since1970-longGiven) + " seconds\n";

		} else {
		    responseText = "Parameter is not numeric. Usage: /" + pathArray[1] + "/LongIntSecondsSince1970\n";
		}
	    }

	    // send content and close request
            c.sendResponseHeaders(200, responseText.length());
            OutputStream rs = c.getResponseBody();
            rs.write(responseText.getBytes());
            rs.close();

       }
    }


    /*
     *-- generate response content (json, yaml, text)
     */
    static String generateResponse(String style) {

	// describe timestamp as seconds since unix epoch
	String secondsSinceEpoch = Long.toString( java.time.Instant.now().getEpochSecond() );
	String ISOdatestamp = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
	String responseContent = "";
	switch (style) {
	    case "json": responseContent += "{\n \"secondsSInce1970\": " + secondsSinceEpoch + ",\n  \"ISOdatestamp\": \"" + ISOdatestamp + "\"\n}\n"; break;
	    case "yaml": responseContent += "---\nsecondsSInce1970: "    + secondsSinceEpoch + "\nISOdatestamp: "        + ISOdatestamp + "\n";    break;
	    default:     responseContent += "secondsSInce1970="          + secondsSinceEpoch + "\nISOdatestamp="         + ISOdatestamp + "\n";
	}
	return responseContent;
    }


    /*
     * handler to print timestamp in different file formats (plain text, json, yaml)
     */
    static class timeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    // get the context path and split it up; [1]=>top context, [2]=>LongInt parameter
	    String pathCalled = c.getRequestURI().getPath();
	    String[] pathArray  = pathCalled.split("[/]");

	    // default format is plain text
	    String style = "text";
	    String cType = "text/plain";

	    // set correct content type
	    if (pathArray.length == 3) {
		style = pathArray[2];
		switch (style) {
		    case "json": cType = "application/json";    break;
		    case "yaml": cType = "application/x-yaml";  break;
		}
	    }

	    // generate the content
	    String responseText = generateResponse(style);

	    // send content and close request
	    c.getResponseHeaders().set("Content-Type", cType);
            c.sendResponseHeaders(200, responseText.length());
            OutputStream rs = c.getResponseBody();
            rs.write(responseText.getBytes());
            rs.close();
	}
    }

}
