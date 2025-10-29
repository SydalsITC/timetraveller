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

    // list of environment variables which may be processed by the envHandler()
    public static List<String> validEnvVars = List.of( "FAKETIME", "LD_PRELOAD" );

    // path to the static text files
    public static String staticFilePath = "static/";

    // standard http error messages in short for plain text msg
    static String msg_403_Forbidden = "403/forbidden";
    static String msg_404_NotFound  = "404/not found";
    static String msg_500_IntSrvErr = "500/internal server error";

    // main class
    public static void main(String[] args) throws Exception {

        HttpServer timeSrv = HttpServer.create(new InetSocketAddress(8080), 0);

        timeSrv.createContext("/",           new rootHandler() );
        timeSrv.createContext("/text/",	     new textHandler() );

        timeSrv.createContext("/env",        new envHandler()  );
        timeSrv.createContext("/diff",       new diffHandler() );

        timeSrv.createContext("/data",       new dataHandler() );
        timeSrv.createContext("/static/",    new staticFileHandler() );

        timeSrv.setExecutor(null); // creates a default executor
        timeSrv.start();
    }


    /******************************************************
     **
     **  auxiliary functions
     **
     **/

    //"""""""""""""""""""""""""""""""""""""""""""""""""
    // get the uptime of the system/pods
    //
    static String getUptime(){
	RuntimeMXBean rmxBean = ManagementFactory.getRuntimeMXBean();
	return Long.toString(rmxBean.getUptime()/1000);
    }

    //""""""""""""""""""""""""""""""""""""""""""""""""""""""
    // determine content type from file.suffix
    //
    static String getContentType(String fileName){
	String[] filenameSplit = fileName.split("[.]");
	String   fileType = filenameSplit[ filenameSplit.length-1 ];

	String cType = "text/plain";	// default value
	switch(fileType){
	    case "html"  : cType = "text/html";		break;
	    case "css"   : cType = "text/css";		break;
	    case "js"    : cType = "text/js";		break;
	}
	return cType;
    }

    //"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""
    //  get parameter from "/context/parameter"
    //
    static String parmFromPathCalled(HttpExchange c){
	// get the context path and split it up; [1]=>top context, [2]=>LongInt parameter
	String  pathCalled = c.getRequestURI().getPath();
	String[] pathArray = pathCalled.split("[/]");
	String   parameter = null;

	if (pathArray.length == 3) {	// MUST be 3 exactly in plain "/context/parameter" model
	    parameter = pathArray[2];
	}
	return parameter;
    }

    //"""""""""""""""""""""""""""""""""""""""""""""""""""""""""
    //  send text content to client
    //
    static void sendTextContent(HttpExchange c, String content, String cType, int responseCode) throws IOException {
	// send content and close request
	c.getResponseHeaders().set("Content-Type", cType);
	c.sendResponseHeaders(responseCode, content.length());
	OutputStream rs = c.getResponseBody();
	rs.write(content.getBytes());
	rs.close();
    }


    /**************************************************************************************
     **
     **  file handling section
     **
     **/

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
        int    responseCode = 404;

	if (responseText == null){
	    responseText = msg_404_NotFound;
	} else {
	    contentType  = getContentType(fileName);
	    responseCode = 200;
	}
	sendTextContent(c, responseText, contentType, responseCode);
    }


    /*
     * handler to send content on /
     */
    static class rootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {
	    sendTextFileContent(c, "root.html");
	}
    }

    /*
     * handler to send content of requested text file
     */
    static class staticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    // default format is plain text
	    String contentType  = "text/plain";
	    String responseText = msg_404_NotFound;
	    int    responseCode = 404;

	    String fileName = parmFromPathCalled(c);
	    if (fileName != null) {
		// send file's content
		responseCode = 200;
		contentType  = getContentType(fileName);
		sendTextFileContent(c, fileName);
	    } else {
		// else: deny (f.r.o.s)
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

	    // default format is plain text
	    String contentType  = "text/plain";
	    String responseText = "";
	    int    responseCode = 200;

            String envVar = parmFromPathCalled(c);

	    if (envVar != null) {
		if (validEnvVars.contains(envVar)) {
		    responseText = System.getenv(envVar);
		} else {
		    responseText = msg_403_Forbidden;
		    responseCode = 403;
		}
	    } else {
		responseText = msg_404_NotFound;
		responseCode = 404;
	    }
            sendTextContent(c, responseText, contentType, responseCode);
	}
    }

    /*
     *  handler to send a simple text, based on text parameter
     */
    static class textHandler implements HttpHandler {
        @Override
	public void handle(HttpExchange c) throws IOException {

	    String parameterTxt = parmFromPathCalled(c); // get parameter from called path
	    if (parameterTxt == null) {   // if null, define unsused parameter
		parameterTxt = "send404"; // which is catched by the default later
	    }

	    int    responseCode = 200;	// det soem defaults
            String responseText = "";
	    String contentType  = "text/plain";

	    // if given parameter is one of the following, then send special response
	    switch(parameterTxt) {

		case "now":
		    responseText = java.time.Instant.now().toString();
		break;

		case "epoch":
		    responseText = Long.toString( java.time.Instant.now().getEpochSecond() );
		break;

		case "uptime":
		    responseText = getUptime();
		break;

		case "java.version":
		    responseText = System.getProperty("java.version");
		break;

		default:
		    responseText = "404 - not found";
		    responseCode = 404;
	    }
	    sendTextContent(c, responseText, contentType, responseCode);
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
            String contentType  = "text/plain";
	    int    responseCode = 200;
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
	    sendTextContent(c, responseText, contentType, responseCode);
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
    static class dataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    // get the context path and split it up; [1]=>top context, [2]=>LongInt parameter
	    String   pathCalled = c.getRequestURI().getPath();
	    String[] pathArray  = pathCalled.split("[/]");

	    // default format is plain text
	    String style       = "text";
	    int responseCode   = 200;
	    String contentType = "text/plain";

	    // set correct content type
	    if (pathArray.length == 3) {
		style = pathArray[2];
		switch (style) {
		    case "json": contentType = "application/json";	break;
		    case "yaml": contentType = "application/x-yaml";	break;
		    case "csv" : contentType = "text/csv";		break;
		}
	    }

	    // generate the content
	    String responseText = generateResponse(style);

	    // send content and close request
	    sendTextContent(c, responseText, contentType, responseCode);
	}
    }
}
