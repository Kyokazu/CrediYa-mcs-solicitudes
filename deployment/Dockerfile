# Etapa de construcción
FROM gradle:8.10.2-jdk21-alpine AS builder
WORKDIR /home/gradle/project
COPY . .
RUN gradle clean build -x test --no-daemon

# Etapa de ejecución
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copiamos el JAR desde la carpeta raíz build/libs
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]