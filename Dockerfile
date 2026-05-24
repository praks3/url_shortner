# syntax=docker/dockerfile:1

# ---- Stage 1: build the fat jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and POM first so dependency resolution is cached
# in its own image layer and only re-runs when pom.xml or the wrapper change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# macOS source trees may not preserve the +x bit on mvnw inside the build context.
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy sources and build. Tests are skipped here; the container build is
# for packaging, not validation — run `./mvnw test` locally for that.
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---- Stage 2: minimal runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Spring Boot produces both <artifact>.jar and <artifact>.jar.original; the
# wildcard plus a rename keeps this independent of the project version string.
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
