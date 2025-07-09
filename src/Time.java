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
 * configuration section
 */

int serverPort 8080;


/*
 * ==== code =============================
 */

public class Time {

    // main class
    public static void main(String[] args) throws Exception {
        HttpServer timeSrv = HttpServer.create(new InetSocketAddress(serverPort), 0);

        timeSrv.createContext("/ss1970", new SecondsSince1970Handler());

        server.setExecutor(null); // creates a default executor
        server.start();
    }

    // handler to print unix epoch/seconds since 1970
    static class SecondsSince1970Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange c) throws IOException {

	    long secondsSinceEpoch = java.time.Instant.now().getEpochSecond();

            String responseText = Long.toString(secondsSinceEpoch) + "\n";

            c.sendResponseHeaders(200, response.length());
            OutputStream rs = c.getResponseBody();
            rs.write(responseText.getBytes());
            rs.close();
        }
    }
}
