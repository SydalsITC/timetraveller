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


/*
 * ==== code =============================
 */

public class Time {

    static String rootText  = "<html><body><h1>timetraveller</h1>\n<p>For more information, please read <a href='/about'>/about</a>.</p></body></html>\n";
    static String aboutText = "<html><body>\n<h1>timetraveller</h1>\n<p>This is a simple web server that answers with the current time as seconds since 1970 on <a href='/epoch'>/epoch</a>. It's intended to be used as test object when shifting kubernetes/openshift containers in time.</p>\n</body></html>\n";

    // main class
    public static void main(String[] args) throws Exception {
        HttpServer timeSrv = HttpServer.create(new InetSocketAddress(8080), 0);

        timeSrv.createContext("/",      new textHandler(rootText)  );
        timeSrv.createContext("/about", new textHandler(aboutText) );
        timeSrv.createContext("/epoch", new secondsSince1970Handler() );

        timeSrv.setExecutor(null); // creates a default executor
        timeSrv.start();
    }


    // handler to send a simple text
    static class textHandler implements HttpHandler {

	private final String responseText;

	public textHandler(String responseText) {
	    this.responseText = responseText;
	}

	public void handle(HttpExchange c) throws IOException {
	    c.sendResponseHeaders(200, responseText.length());
	    OutputStream rs = c.getResponseBody();
	    rs.write(responseText.getBytes());
	    rs.close();
	}
    }


    // handler to print unix epoch/seconds since 1970
    static class secondsSince1970Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    long secondsSinceEpoch = java.time.Instant.now().getEpochSecond();

            String responseText = Long.toString(secondsSinceEpoch) + "\n";

            c.sendResponseHeaders(200, responseText.length());
            OutputStream rs = c.getResponseBody();
            rs.write(responseText.getBytes());
            rs.close();
        }
    }

}
