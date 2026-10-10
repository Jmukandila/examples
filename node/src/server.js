import { createApp } from './app.js';

const port = Number(process.env.PORT || 3000);
const server = createApp().listen(port, () => {
  console.log(`listening on :${port}`);
});

// Miabi stops a release with SIGTERM; finish in-flight requests before exiting.
for (const signal of ['SIGTERM', 'SIGINT']) {
  process.on(signal, () => {
    console.log(`${signal} received, shutting down`);
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(1), 10_000).unref();
  });
}
