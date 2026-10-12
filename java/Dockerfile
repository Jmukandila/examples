# syntax=docker/dockerfile:1
# Bytecode is platform-independent, so compiling runs natively; only jlink runs on the target platform.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# A runtime with only the modules the app uses: about 40 MB instead of a 200 MB JRE.
FROM eclipse-temurin:25-jdk AS runtime
RUN jlink --add-modules java.base,java.management,jdk.httpserver \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output /jre

FROM gcr.io/distroless/base-debian12:nonroot
COPY --from=runtime /jre /jre
COPY --from=build /src/target/app.jar /app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["/jre/bin/java", "-jar", "/app.jar"]
