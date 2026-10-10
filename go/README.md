# Go example

An API built with [Okapi](https://github.com/jkaninda/okapi), compiled to a static binary on
[distroless](https://github.com/GoogleContainerTools/distroless). The image has no shell and
runs as uid `65532`, so the [manifest](../gitops/go/app.yaml) drops every capability and
forbids new privileges.

The root filesystem stays writable. Miabi's `http` health check copies a small probe into the
container. With `readOnlyRootFilesystem: true` it can't, and it falls back to `curl` or `wget`
from the image, which distroless doesn't have, so every release would report unhealthy.

| Route | |
|---|---|
| `/` | A page naming the version, architecture and the container that served it |
| `/healthz` | `{"status":"ok"}`, the health check |
| `/api/info` | Version, hostname, Go version, `os/arch`, uptime |
| `/docs` | OpenAPI docs Okapi generates from the routes |

`PORT` (default `8080`) and `APP_NAME` are read from the environment.

## Versioning

The binary reports `VERSION` plus the short commit, such as `1.0.0-3f9c2a1`. A pipeline's
`build` step sees no environment, so the [pipeline](.miabi/pipeline.yaml) writes the version to
`.version` in an earlier step, and the [Dockerfile](Dockerfile) reads it. A local build without
it falls back to `VERSION`.

## Multi-arch

The Dockerfile compiles on the build machine's own platform and cross-compiles to the target,
so `platforms: [linux/amd64, linux/arm64]` builds both without emulation. It needs runner
0.0.11 or later.

## Run it

```bash
go test ./... && go run .
```

The tests use Okapi's `okapitest` client against an `httptest` server.

```bash
curl localhost:8080/openapi.json
```

## Deploy it

- **From an image:** `miabi apply -f ../gitops/go/app.yaml`.
- **From source:** copy this folder into its own repository and create a Git app from it.
  Miabi offers to adopt [`.miabi/pipeline.yaml`](.miabi/pipeline.yaml): test, lint, version,
  multi-arch build, deploy.
