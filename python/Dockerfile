# syntax=docker/dockerfile:1
FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PORT=8000

WORKDIR /app
COPY requirements.txt ./
RUN pip install -r requirements.txt
COPY app ./app

RUN useradd --system --uid 10001 app
USER 10001
EXPOSE 8000
# --proxy-headers: TLS ends at the Miabi gateway, so trust its X-Forwarded-* headers.
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT} --proxy-headers --forwarded-allow-ips='*'"]
