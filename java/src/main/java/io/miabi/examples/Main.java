package io.miabi.examples;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/** A plain Java service deployed on Miabi: the JDK's HTTP server and nothing else. */
public final class Main {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = App.fromEnv().start(new InetSocketAddress(port));
        // stop() waits for in-flight exchanges, up to the delay, before Docker's 10-second kill.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(5)));
        System.out.println("listening on :" + server.getAddress().getPort());
    }
}
