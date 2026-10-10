// Command server is a small Okapi service deployed on Miabi.
package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"strconv"
	"syscall"
	"time"

	"github.com/jkaninda/okapi"
)

// version is stamped at build time with -ldflags "-X main.version=...".
var version = "dev"

type Health struct {
	Status string `json:"status" example:"ok"`
}

type Info struct {
	App           string `json:"app"`
	Version       string `json:"version"`
	Hostname      string `json:"hostname"`
	Go            string `json:"go"`
	Arch          string `json:"arch"`
	UptimeSeconds int64  `json:"uptimeSeconds"`
}

func main() {
	port, err := strconv.Atoi(envOr("PORT", "8080"))
	if err != nil {
		slog.Error("invalid PORT", "err", err)
		os.Exit(1)
	}
	app := newApp(time.Now()).WithPort(port)

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	go func() {
		slog.Info("listening", "port", port, "version", version)
		if err := app.Start(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			slog.Error("server failed", "err", err)
			os.Exit(1)
		}
	}()

	<-ctx.Done()
	slog.Info("shutting down")
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := app.StopWithContext(shutdownCtx); err != nil {
		slog.Error("shutdown", "err", err)
	}
}

func newApp(started time.Time) *okapi.Okapi {
	hostname, _ := os.Hostname()
	current := func() Info {
		return Info{
			App:           envOr("APP_NAME", "go-example"),
			Version:       version,
			Hostname:      hostname,
			Go:            runtime.Version(),
			Arch:          runtime.GOOS + "/" + runtime.GOARCH,
			UptimeSeconds: int64(time.Since(started).Seconds()),
		}
	}

	app := okapi.New().WithOpenAPIDocs(okapi.OpenAPI{Title: "go-example", Version: version})

	app.Get("/healthz", func(c *okapi.Context) error {
		return c.OK(Health{Status: "ok"})
	}, okapi.DocSummary("Health check"), okapi.DocResponse(&Health{}))

	app.Get("/api/info", func(c *okapi.Context) error {
		return c.OK(current())
	}, okapi.DocSummary("Build and runtime details"), okapi.DocResponse(&Info{}))

	app.Get("/", func(c *okapi.Context) error {
		i := current()
		return c.HTMLView(http.StatusOK, fmt.Sprintf(page, i.App, i.Version, i.Go, i.Arch, i.Hostname), nil)
	}, okapi.DocHide())

	return app
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

const page = `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>%s</title>
<style>body{font:16px/1.5 system-ui,sans-serif;max-width:40rem;margin:4rem auto;padding:0 1rem}code{background:#f2f2f2;padding:.1rem .3rem;border-radius:4px}</style></head>
<body>
<h1>Hello from Go on Miabi</h1>
<p>Version <code>%s</code> · %s · <code>%s</code> · served by <code>%s</code></p>
<p>Built with <a href="https://github.com/jkaninda/okapi">Okapi</a>. Try <a href="/api/info">/api/info</a>, <a href="/healthz">/healthz</a> or the API docs at <a href="/docs">/docs</a>.</p>
</body>
</html>`
