/*
 *
 *  Time.java - simple web server for displaying system time
 *
 *  This software is distributed under the Apache 2.0 license.
 *  For more details see the file LICENSE.
 *
 */

import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

// for retrieving uptime:
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

// file i/o
import java.io.BufferedReader;
import java.io.FileReader;


/*
 * ==== code =============================
 */

public class Time {

    /*****************************************************
     **
     **  definitions
     **
     **/

    public static List<String> validEnvVars = List.of( "FAKETIME", "LD_PRELOAD" );
    public static String     staticFilePath = "static/";

    /******************************************************
     **
     **  auxiliary functions
     **
     **/


    static String getUptime(){
	RuntimeMXBean rmxBean = ManagementFactory.getRuntimeMXBean();
	return Long.toString(rmxBean.getUptime()/1000);
    }

    static String getContentType(String fileName){
	String[] filenameSplit = fileName.split("[.]");
	String fileType = filenameSplit[ filenameSplit.length-1 ];

	String cType = "text/plain";	// default value
	switch(fileType){
	    case "html"  : cType = "text/html";		break;
	    case "css"   : cType = "text/css";		break;
	    case "js"    : cType = "text/js";		break;
	}
	return cType;
    }

    // main class
    public static void main(String[] args) throws Exception {

        HttpServer timeSrv = HttpServer.create(new InetSocketAddress(8080), 0);

        timeSrv.createContext("/",           new rootHandler() );

        timeSrv.createContext("/now",        new textHandler("now") );
        timeSrv.createContext("/epoch",      new textHandler("epoch") );
        timeSrv.createContext("/uptime",     new textHandler("uptime") );

        timeSrv.createContext("/env",        new envHandler()  );
        timeSrv.createContext("/diff",       new diffHandler() );
        timeSrv.createContext("/time",       new timeHandler() );
        timeSrv.createContext("/static/",    new staticFileHandler() );

        timeSrv.setExecutor(null); // creates a default executor
        timeSrv.start();
    }

    /**************************************************************************************
     **
     **  file handling section
     **
     **/

    static void sendTextContent(HttpExchange c, String content, String cType, int responseCode) throws IOException {
	// send content and close request
	c.getResponseHeaders().set("Content-Type", cType);
	c.sendResponseHeaders(responseCode, content.length());
	OutputStream rs = c.getResponseBody();
	rs.write(content.getBytes());
	rs.close();
    }

    static String readTextFile(String fileName){
	String responseText = "";

	try (BufferedReader br = new BufferedReader(new FileReader(staticFilePath + fileName))) {
	    StringBuilder   sb = new StringBuilder();
	    String    textLine = br.readLine();

	    while (textLine != null) {
	        sb.append(textLine);
	        sb.append(System.lineSeparator());
	        textLine = br.readLine();
	    }
	    responseText = sb.toString();

        } catch (IOException e) {
	    responseText = null;
	}
	return responseText;
    }

    static void sendTextFileContent(HttpExchange c, String fileName) throws IOException {
	String responseText = readTextFile(fileName);
	String contentType  = "text/plain";
        int    responseCode = 500;

	if (responseText == null){
	    responseText = "500 - file i/o error";
	} else {
	    contentType  = getContentType(fileName);
	    responseCode = 200;
	}
	sendTextContent(c, responseText, contentType, responseCode);
    }

    /*
     * handler to send content of requested env variable
     */
    static class rootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {
	    sendTextFileContent(c, "root.html");
	}
    }
    /*
     * handler to send content of requested env variable
     */
    static class staticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    // get the context path and split it up; [1]=>top context, [2]=>LongInt parameter
	    String   pathCalled = c.getRequestURI().getPath();
	    String[] pathArray  = pathCalled.split("[/]");

	    // default format is plain text
	    String contentType  = "text/plain";
	    String responseText = "";
	    int    responseCode = 200;

	    // set correct content type
	    if (pathArray.length == 3) {			// MUST be 3 exactly in plain "/static/file" model
		String fileName = pathArray[2];
		sendTextFileContent(c, fileName);
	    } else {
		responseText = "404 - file not found";		// else deny f.r.o.s
		responseCode = 404;
		sendTextContent(c, responseText, contentType, responseCode);
	    }
	}

    } //- end(staticFileHandler)


    /*
     * handler to send content of requested env variable
     */
    static class envHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    // get the context path and split it up; [1]=>top context, [2]=>LongInt parameter
	    String  pathCalled = c.getRequestURI().getPath();
	    String[] pathArray = pathCalled.split("[/]");

	    // default format is plain text
	    String cType  = "text/plain";
	    String responseText = "";
	    int    responseCode = 200;

	    // set correct content type
	    if (pathArray.length == 3) {
		String envVar = pathArray[2];
		if (validEnvVars.contains(envVar)) {
		    responseText = System.getenv(envVar);
		} else {
		    responseText = "403/Forbidden";
		    responseCode = 403;
		}
	    } else {
		responseText = "404/Not found";
		responseCode = 404;
	    }

	    // send content and close request
	    c.getResponseHeaders().set("Content-Type", cType);
            c.sendResponseHeaders(responseCode, responseText.length());
            OutputStream rs = c.getResponseBody();
            rs.write(responseText.getBytes());
            rs.close();
	}
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

		case "uptime":
		    finalResponseText = getUptime();
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

	    case "json": responseContent += "{\n  \"uptime\": "		+ getUptime()
					 +  ",\n  \"epoch\": " 		+ secondsSinceEpoch
					 +  ",\n  \"now\": \""		+ ISOdatestamp
					 +  "\",\n  \"FAKETIME\": \""   + System.getenv("FAKETIME")
					 +  "\",\n  \"LD_PRELOAD\": \"" + System.getenv("LD_PRELOAD")
					 +  "\"\n}\n";
			 break;

	    case "yaml": responseContent += "---"
					 +  "\nuptime: "	+ getUptime()
					 +  "\nepoch: "		+ secondsSinceEpoch
					 +  "\nnow: "		+ ISOdatestamp
					 +  "\nFAKETIME: "    	+ System.getenv("FAKETIME")
					 +  "\nLD_PRELOAD: "	+ System.getenv("LD_PRELOAD")
					 +  "\n";
			 break;

	    case "csv" : responseContent += "uptime,epoch,now,FAKETIME,LD_PRELOAD"
					 +  "\n"		+ getUptime()
					 +  ","			+ secondsSinceEpoch
					 +  ","			+ ISOdatestamp
					 +  ","			+ System.getenv("FAKETIME")
					 +  ","			+ System.getenv("LD_PRELOAD")
					 +  "\n";
			 break;

	    default:     responseContent += "uptime="		+ getUptime()
					 +  "\nepoch="		+ ISOdatestamp
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
