package test.stackimpact.agent;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TestAPI {
    private static HttpServer server;

    private static String requestPayload;
    private static String responsePayload;

    public static void start(int port, String endpoint) throws Exception {
        requestPayload = "{}";
        responsePayload = "{}";

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/agent/v1/" + endpoint, new EndpointHandler());
        server.setExecutor(null);
        server.start();
    }


    public static void stop() throws Exception {
        server.stop(0);
    }

    static class EndpointHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                if(t.getRequestHeaders().get("Content-encoding") !=null &&
                        "gzip".equals(t.getRequestHeaders().get("Content-encoding").get(0))) {

                    InputStream is = t.getRequestBody();
                    GZIPInputStream gzis = new GZIPInputStream(is);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (int value = 0; value != -1; ) {
                        value = gzis.read();
                        if (value != -1) {
                            baos.write(value);
                        }
                    }

                    requestPayload = new String(baos.toByteArray(), "UTF-8");
                    gzis.close();
                    baos.close();
                }

                String response = responsePayload;

                ByteArrayOutputStream baosGzip = new ByteArrayOutputStream();
                GZIPOutputStream gzos = null;
                gzos = new GZIPOutputStream(baosGzip);
                gzos.write(response.getBytes("UTF-8"));
                gzos.close();

                byte[] compressedPayload = baosGzip.toByteArray();
                baosGzip.close();

                Headers headers = t.getResponseHeaders();
                headers.add("Content-Encoding", "gzip");
                headers.add("Content-Type", "application/json");
                t.sendResponseHeaders(200, compressedPayload.length);
                OutputStream os = t.getResponseBody();
                os.write(compressedPayload);
                os.close();
            }
            catch (Exception ex) {
                ex.printStackTrace();

                String response = "{}";
                Headers headers = t.getResponseHeaders();
                headers.add("Content-Type", "application/json");
                t.sendResponseHeaders(500, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }


    public static String getRequestPayload() {
        return requestPayload;
    }


    public static String getResponsePayload() {
        return responsePayload;
    }

    public static void setResponsePayload(String responsePayload) {
        TestAPI.responsePayload = responsePayload;
    }

}