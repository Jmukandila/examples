# GitOps examples

Each folder is a complete desired state for one [Git source](https://docs.miabi.io/docs/cicd/gitops).
Point a source at a folder and Miabi converges the workspace to it: on every three-minute sweep
for an `auto` source, or when you press **Sync now**. Every `.yaml` file under the folder is
read as one bundle, so a folder can be split into files however reads best.

| Folder | Declares |
|---|---|
| [`node`](node) | One app with a public URL and an HTTP health check |
| [`go`](go) | One app with every capability dropped, running as non-root |
| [`python`](python) | An app and the managed Postgres it connects to |
| [`php`](php) | An app and a managed Redis |
| [`laravel`](laravel) | Postgres, Redis, a generated `APP_KEY`, the app, a migration Job and a CronJob |
| [`full-stack`](full-stack) | A `Project`: front end, API and database, a custom domain, a route with a rate limit |
| [`environments`](environments) | The same app as `staging` (follows `latest`) and `production` (pinned) |

The images come from `ghcr.io/miabi-io/examples/<name>`, published by this repository's
[images workflow](../.github/workflows/images.yml).

## Try one without Git

The same files work as a one-shot apply. Read the plan first:

```bash
miabi apply $(printf -- '-f %s ' gitops/laravel/*.yaml) --dry-run
miabi apply $(printf -- '-f %s ' gitops/laravel/*.yaml)
miabi delete $(printf -- '-f %s ' gitops/laravel/*.yaml)   # removes exactly what they declare
```

`-f` takes one file and can be repeated; all of them are read as one bundle.

## Connect a Git source

Register `https://github.com/miabi-io/examples` under **Git repositories** (it is public, so
no token is needed). Then open **GitOps → New git source**, select the repository, and set the
path to a folder, such as `gitops/laravel`. Or over the API, with the URL directly:

```bash
curl -X POST "$MIABI_SERVER/api/v1/workspaces/$WORKSPACE/gitops" \
  -H "Authorization: Bearer $MIABI_TOKEN" -H 'Content-Type: application/json' \
  -d '{
        "name": "laravel",
        "repo_url": "https://github.com/miabi-io/examples",
        "ref": "main",
        "path": "gitops/laravel",
        "sync_policy": "auto",
        "prune": true,
        "self_heal": true
      }'
```

| Option | Effect |
|---|---|
| `sync_policy: auto` | Sync on the three-minute sweep. `manual` syncs only on request |
| `prune` | Delete what this source created once it disappears from Git |
| `self_heal` | Undo changes made outside Git, such as an env var edited in the console |

Prune only removes resources this source created, never a hand-made app or another source's.

To change anything, fork the repository, point the source at your fork, and commit.

## What each folder teaches

**Wiring without credentials.** The apps get their connection strings by interpolation. Nothing
secret is written in Git:

```yaml
env:
  DATABASE_URL: "{{ .databases.python-example-db.uri }}"   # resolved at apply time
  APP_KEY: "{{ .secrets.laravel-app-key }}"                 # a Secret with generate: true
  UPSTREAM_URL: "{{ .applications.notes-api }}"             # http://notes-api:8000
secretEnv: [DATABASE_URL, APP_KEY]                          # stored encrypted at rest
```

**Ordering.** Volumes, secrets and domains are created first, then databases, then apps, then
routes and jobs, so each folder converges in a single sync.

**Migrations.** [`laravel/jobs.yaml`](laravel/jobs.yaml) declares a Job with
`runPolicy: onRelease`. It runs `php artisan migrate --force` each time a new release of the app
goes live, whether the release came from this source, the console, a pipeline or a rollback.
Applying the same spec again never runs it twice.

**A custom domain.** [`full-stack`](full-stack/project.yaml) declares a `Domain`, a rate-limit
`Middleware` and a `Route` with automatic TLS. Change `notes.example.com` to a host you own,
point its DNS at your gateway, then verify the domain in the console: verification is a runtime
step, not something a manifest can declare.

**Promotion.** [`environments`](environments) holds the same app twice. Connect
`environments/staging` to a staging workspace and `environments/production` to a production one.
Staging runs `latest`; production moves only when a commit changes its `tag`, so every
production change is a reviewed, revertible commit.

## Secrets you want in Git

A `Secret` with `generate: true` never appears in Git, which suits keys nobody needs to know. For
a value you do need to supply, such as an API token, seal it to the workspace's key and commit
the sealed form:

```bash
echo -n "$STRIPE_KEY" | miabi secrets seal stripe-key > gitops/my-app/stripe-key.yaml
```

See [Sealed secrets](https://docs.miabi.io/docs/secrets/sealed-secrets).
