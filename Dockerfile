# Builds and runs the start-site Spring Boot + Vaadin app (executable fat jar).
# Build context is the repository ROOT so the multi-module reactor's parent POM resolves.
# Multi-stage: build the Vaadin production bundle with Maven, run on a slim JRE.

# --- build stage: reactor build incl. Vaadin production frontend bundle ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# -pl start-site -am builds the parent POM + start-site only (test-support is test-scope
# and skipped via maven.test.skip). -Pproduction runs Vaadin build-frontend; the
# vaadin-maven-plugin auto-provisions the Node/Vite toolchain during the build.
# maven.gitcommitid.skip skips git-commit-id-maven-plugin: the .git dir is not in the
# build context (see .dockerignore) and it only feeds actuator /info metadata.
RUN mvn -B -Pproduction -pl start-site -am -Dmaven.test.skip=true -Dmaven.gitcommitid.skip=true clean package

# --- runtime stage: slim JRE runs the executable fat jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Container-aware heap sizing so the JVM respects the container's RAM limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# The app listens on Spring Boot's default port 8080.
EXPOSE 8080
# repackage adds the "exec" classifier, so start-site-exec.jar is the runnable fat jar.
COPY --from=build /app/start-site/target/start-site-exec.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]
