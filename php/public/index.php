<?php

declare(strict_types=1);

require __DIR__.'/../src/App.php';

$env = getenv();
$app = new App($env, App::connectRedis($env['REDIS_URL'] ?? null));

[$status, $type, $body] = $app->handle(
    $_SERVER['REQUEST_METHOD'] ?? 'GET',
    parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/',
);

http_response_code($status);
header('Content-Type: '.$type);
echo $body;
