package io.miabi.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DatabaseUrlTest {

    @Test
    void splitsMiabiUriIntoJdbcUrlAndCredentials() {
        DatabaseUrl db = DatabaseUrl.parse("postgres://notes:s3cr%40t+x@db-host:5433/notes?sslmode=disable");
        assertThat(db.jdbcUrl()).isEqualTo("jdbc:postgresql://db-host:5433/notes?sslmode=disable");
        assertThat(db.username()).isEqualTo("notes");
        assertThat(db.password()).isEqualTo("s3cr@t+x");
    }

    @Test
    void defaultsThePort() {
        assertThat(DatabaseUrl.parse("postgresql://u:p@db/app").jdbcUrl())
                .isEqualTo("jdbc:postgresql://db:5432/app");
    }

    @Test
    void unsetMeansNoDatabase() {
        assertThat(DatabaseUrl.fromEnv(null)).isEmpty();
        assertThat(DatabaseUrl.fromEnv(" ")).isEmpty();
    }

    @Test
    void rejectsOtherSchemes() {
        assertThatThrownBy(() -> DatabaseUrl.parse("mysql://u:p@db/app"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
