FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/shorts-blocker-kids/billing-backend

COPY billing-backend/build/install/billing-backend/ ./

EXPOSE 8080

ENTRYPOINT ["bin/billing-backend"]
