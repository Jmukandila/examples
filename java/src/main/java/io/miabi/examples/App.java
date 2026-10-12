package io.miabi.examples;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** The routes, built on {@link HttpServer} so the image needs no dependencies. */
public record App(String name, String version) {

    public static App fromEnv() {
        String version = System.getenv("APP_VERSION");
        if (version == null || version.isBlank()) {
            version = App.class.getPackage().getImplementationVersion();
        }
        return new App(
                System.getenv().getOrDefault("APP_NAME", "java-example"),
                version == null ? "dev" : version);
    }

    public HttpServer start(InetSocketAddress address) throws IOException {
        HttpServer server = HttpServer.create(address, 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/healthz", get(ex -> send(ex, 200, "application/json", "{\"status\":\"ok\"}")));
        server.createContext("/api/info", get(ex -> send(ex, 200, "application/json", json(info()))));
        server.createContext("/", get(this::index));
        server.start();
        return server;
    }

    Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", name);
        info.put("version", version);
        info.put("hostname", hostname());
        info.put("java", Runtime.version().toString());
        info.put("platform", System.getProperty("os.name").toLowerCase() + "/" + System.getProperty("os.arch"));
        info.put("processors", Runtime.getRuntime().availableProcessors());
        info.put("maxHeapMiB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        info.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return info;
    }

    private void index(HttpExchange ex) throws IOException {
        // The "/" context also catches every path no other context matches.
        if (!ex.getRequestURI().getPath().equals("/")) {
            send(ex, 404, "application/json", "{\"error\":\"not found\"}");
            return;
        }
        Map<String, Object> i = info();
        String page = """
                <!doctype html>
                <html lang="en">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>%s</title>
                <style>body{font:16px/1.5 system-ui,sans-serif;max-width:40rem;margin:4rem auto;padding:0 1rem}code{background:#f2f2f2;padding:.1rem .3rem;border-radius:4px}</style></head>
                <body>
                <h1>Hello from Java on Miabi</h1>
                <p>Version <code>%s</code> · Java %s · %s · served by <code>%s</code></p>
                <p>Try <a href="/api/info">/api/info</a> or <a href="/healthz">/healthz</a>.</p>
                </body>
                </html>
                """.formatted(html(i.get("app")), html(i.get("version")), html(i.get("java")),
                html(i.get("platform")), html(i.get("hostname")));
        send(ex, 200, "text/html; charset=utf-8", page);
    }

    private static HttpHandler get(HttpHandler next) {
        return ex -> {
            try (ex) {
                String method = ex.getRequestMethod();
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    ex.getResponseHeaders().set("Allow", "GET, HEAD");
                    send(ex, 405, "application/json", "{\"error\":\"method not allowed\"}");
                    return;
                }
                next.handle(ex);
            }
        };
    }

    private static void send(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        boolean head = ex.getRequestMethod().equals("HEAD");
        ex.sendResponseHeaders(status, head ? -1 : bytes.length);
        if (!head) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    static String json(Map<String, Object> map) {
        return map.entrySet().stream()
                .map(e -> quote(e.getKey()) + ":" + (e.getValue() instanceof Number n ? n.toString() : quote(String.valueOf(e.getValue()))))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }

    private static String html(Object value) {
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
