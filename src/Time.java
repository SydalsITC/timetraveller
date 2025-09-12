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

    static String headText  = "<html><head><style>body{font-family: Arial, sans-serif;}</style></head><body>\n<h1>timetraveller app</h1>\n";
    static String rootText  = headText + "<p>For more information, please read <a href='/about'>/about</a>.</p></body></html>\n";
    static String aboutText = headText + "<p>This is a simple web server that's intended to be used as test object when shifting kubernetes/openshift containers in time.</p>\n<ul>\n  <li/>It shows the current time as text at <a href='/now'>/now</a> and as seconds since 1970 at <a href='/epoch'>/epoch</a>.\n  <li/>On /diff/<i>yourTimeInSecondsSince1970</i> you'll find the difference between given and internal time.\n  <li/>Find more output under /time/xxx in different formats; see Readme.md for details.\n  <li/>At <a href='/FAKETIME'>/FAKETIME</a> and <a href='/LD_PRELOAD'>/LD_PRELOAD</a> it displays the content of the respective environment variable.\n</ul>\n</body></html>\n";

    // main class
    public static void main(String[] args) throws Exception {
        HttpServer timeSrv = HttpServer.create(new InetSocketAddress(8080), 0);

        timeSrv.createContext("/",           new textHandler(rootText)  );
        timeSrv.createContext("/about",      new textHandler(aboutText) );
        timeSrv.createContext("/now",        new textHandler("now") );
        timeSrv.createContext("/epoch",      new textHandler("epoch") );
        timeSrv.createContext("/FAKETIME",   new textHandler("FAKETIME") );
        timeSrv.createContext("/LD_PRELOAD", new textHandler("LD_PRELOAD") );
        timeSrv.createContext("/diff",       new diffHandler() );
        timeSrv.createContext("/time",       new timeHandler() );

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
	    // if given response is one of the following, then send special response
	    switch(finalResponseText) {
		case "now":
		    finalResponseText = java.time.Instant.now().toString();
		break;
		case "epoch":
		    finalResponseText = Long.toString( java.time.Instant.now().getEpochSecond() );
		break;
		case "FAKETIME":
		    finalResponseText = System.getenv("FAKETIME");
		break;
		case "LD_PRELOAD":
		    finalResponseText = System.getenv("LD_PRELOAD");
		break;
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

	    case "json": responseContent += "{\n  \"epoch\": " 		+ secondsSinceEpoch
					 +  ",\n  \"now\": \""		+ ISOdatestamp
					 +  "\",\n  \"FAKETIME\": \""   + System.getenv("FAKETIME")
					 +  "\",\n  \"LD_PRELOAD\": \"" + System.getenv("LD_PRELOAD")
					 +  "\"\n}\n";
			 break;

	    case "yaml": responseContent += "---"
					 +  "\nepoch: "		+ secondsSinceEpoch
					 +  "\nnow: "		+ ISOdatestamp
					 +  "\nFAKETIME: "    	+ System.getenv("FAKETIME")
					 +  "\nLD_PRELOAD: "	+ System.getenv("LD_PRELOAD")
					 +  "\n";
			 break;

	    case "csv" : responseContent += "epoch,now,FAKETIME,LD_PRELOAD"
					 +  "\n"		+ secondsSinceEpoch
					 +  ","			+ ISOdatestamp
					 +  ","			+ System.getenv("FAKETIME")
					 +  ","			+ System.getenv("LD_PRELOAD")
					 +  "\n";
			 break;

	    default:     responseContent += "epoch="		+ secondsSinceEpoch
					 +  "\nnow="		+ ISOdatestamp
					 +  "\nFAKETIME="	+ System.getenv("FAKETIME")
					 +  "\nLD_PRELOAD="	+ System.getenv("LD_PRELOAD")
					 +  "\n";
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
		    case "csv" : cType = "text/csv";  		break;
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
