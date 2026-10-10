package main

import (
	"net/http/httptest"
	"testing"
	"time"

	"github.com/jkaninda/okapi/okapitest"
)

func newTestServer(t *testing.T) string {
	t.Helper()
	srv := httptest.NewServer(newApp(time.Now()))
	t.Cleanup(srv.Close)
	return srv.URL
}

func TestHealthz(t *testing.T) {
	okapitest.GET(t, newTestServer(t)+"/healthz").
		ExpectStatusOK().
		ExpectJSONPath("status", "ok")
}

func TestInfoReportsVersion(t *testing.T) {
	version = "1.2.3"
	t.Cleanup(func() { version = "dev" })

	okapitest.GET(t, newTestServer(t)+"/api/info").
		ExpectStatusOK().
		ExpectJSONPath("version", "1.2.3").
		ExpectJSONPath("app", "go-example")
}

func TestIndexRenders(t *testing.T) {
	okapitest.GET(t, newTestServer(t)+"/").
		ExpectStatusOK().
		ExpectBodyContains("Hello from Go on Miabi")
}

func TestUnknownPathIsNotFound(t *testing.T) {
	okapitest.GET(t, newTestServer(t)+"/missing").ExpectStatusNotFound()
}
