package io.miabi.examples;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
class NoteController {

    record NoteIn(@NotBlank @Size(max = 500) String body) {}

    record Note(long id, String body, OffsetDateTime createdAt) {}

    private final JdbcClient jdbc;

    NoteController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    List<Note> list() {
        return jdbc.sql("SELECT id, body, created_at FROM notes ORDER BY id DESC LIMIT 50")
                .query(Note.class)
                .list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Note create(@Valid @RequestBody NoteIn in) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO notes (body) VALUES (?)").param(in.body()).update(keys, "id");
        return jdbc.sql("SELECT id, body, created_at FROM notes WHERE id = ?")
                .param(keys.getKeyAs(Long.class))
                .query(Note.class)
                .single();
    }
}
