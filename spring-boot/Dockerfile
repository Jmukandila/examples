# syntax=docker/dockerfile:1
# Bytecode is platform-independent, so the build stage runs natively and multi-arch builds need no emulation.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination /out

# No RUN in this stage: it builds for the target platform, which would need emulation.
FROM eclipse-temurin:25-jre
WORKDIR /app
# Dependencies change least often, so they get their own layers and a new release ships only the app layer.
COPY --from=build /out/dependencies/ ./
COPY --from=build /out/spring-boot-loader/ ./
COPY --from=build /out/snapshot-dependencies/ ./
COPY --from=build /out/application/ ./
USER 10001
EXPOSE 8080
# Size the heap from the container's memory limit, not the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
