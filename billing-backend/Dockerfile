FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/shorts-blocker-kids/billing-backend

COPY billing-backend/build/install/billing-backend/ ./
COPY billing-backend/migrations/ ./migrations/

RUN useradd --system --create-home --home-dir /home/sbk sbk \
    && mkdir -p /data \
    && chown -R sbk:sbk /opt/shorts-blocker-kids /data

EXPOSE 8080

USER sbk

ENTRYPOINT ["bin/billing-backend"]
