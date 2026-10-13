# syntax=docker/dockerfile:1
# Every stage that runs a command runs natively on the build host, so multi-arch builds need no emulation.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# A runtime with only the modules the app uses: about 40 MB instead of a 200 MB JRE.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS runtime
ARG TARGETARCH
RUN arch=$([ "$TARGETARCH" = arm64 ] && echo aarch64 || echo x64) \
 && mkdir /jmods \
 && curl -fsSL "https://api.adoptium.net/v3/binary/version/${JAVA_VERSION}/linux/${arch}/jmods/hotspot/normal/eclipse" \
    | tar xz -C /jmods --strip-components=1 \
 && jlink --module-path /jmods --add-modules java.base,java.management,jdk.httpserver \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output /jre

FROM gcr.io/distroless/base-debian12:nonroot
COPY --from=runtime /jre /jre
COPY --from=build /src/target/app.jar /app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["/jre/bin/java", "-jar", "/app.jar"]
