package io.miabi.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** A small Spring Boot service deployed on Miabi, with a Postgres-backed notes API. */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        DatabaseUrl.fromEnv(System.getenv("DB_URL"))
                .ifPresent(db -> app.setDefaultProperties(db.datasourceProperties()));
        app.run(args);
    }
}
