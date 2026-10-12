package io.miabi.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AppTest {

    static HttpServer server;
    static HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        server = new App("java-example", "1.2.3").start(new InetSocketAddress("127.0.0.1", 0));
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    static HttpResponse<String> request(String method, String path) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        HttpRequest req = HttpRequest.newBuilder(uri).method(method, HttpRequest.BodyPublishers.noBody()).build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthz() throws Exception {
        HttpResponse<String> res = request("GET", "/healthz");
        assertEquals(200, res.statusCode());
        assertEquals("{\"status\":\"ok\"}", res.body());
    }

    @Test
    void infoReportsVersion() throws Exception {
        HttpResponse<String> res = request("GET", "/api/info");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"version\":\"1.2.3\""), res.body());
    }

    @Test
    void indexRenders() throws Exception {
        HttpResponse<String> res = request("GET", "/");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Hello from Java on Miabi"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        assertEquals(404, request("GET", "/nope").statusCode());
    }

    @Test
    void onlyGetAndHead() throws Exception {
        assertEquals(405, request("POST", "/healthz").statusCode());
        assertEquals(200, request("HEAD", "/healthz").statusCode());
    }

    @Test
    void jsonEscapes() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", "x\"y\\z\n");
        m.put("n", 3);
        assertEquals("{\"a\":\"x\\\"y\\\\z\\u000a\",\"n\":3}", App.json(m));
    }
}
