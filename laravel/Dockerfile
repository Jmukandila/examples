# syntax=docker/dockerfile:1
ARG PHP_VERSION=8.4

FROM jkaninda/nginx-php-fpm:${PHP_VERSION} AS vendor
WORKDIR /var/www/html
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist --no-interaction
COPY . .
RUN composer dump-autoload --optimize --no-dev \
 && php artisan route:cache \
 && php artisan view:cache \
 && php artisan event:cache

FROM node:22-alpine AS assets
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
COPY --from=vendor /var/www/html/vendor ./vendor
RUN npm run build

FROM jkaninda/nginx-php-fpm:${PHP_VERSION}
WORKDIR /var/www/html
COPY --from=vendor --chown=www-data:www-data /var/www/html ./
COPY --from=assets --chown=www-data:www-data /app/public/build ./public/build
USER www-data
EXPOSE 80
