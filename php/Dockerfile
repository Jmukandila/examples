# syntax=docker/dockerfile:1
# nginx + PHP-FPM under supervisor; nginx serves /var/www/html/public on port 80.
FROM jkaninda/nginx-php-fpm:8.4
WORKDIR /var/www/html
COPY --chown=www-data:www-data . .
USER www-data
EXPOSE 80
