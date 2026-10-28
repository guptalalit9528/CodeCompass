FROM maven:3.9-eclipse-temurin-25-noble AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app
ENV PORT=10000
RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /app/data \
    && chown -R appuser:appuser /app
COPY --from=build --chown=appuser:appuser /workspace/target/codecompass-1.0.0.jar /app/app.jar

USER appuser
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
