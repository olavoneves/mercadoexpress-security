# =========================================================
# Etapa 1 - build do .jar com Maven
# =========================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa as dependencias primeiro, para aproveitar o cache de camadas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# =========================================================
# Etapa 2 - imagem final, apenas o runtime
# =========================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda sem privilegios de root dentro do container
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app

# Credenciais chegam por variavel de ambiente (painel do Render ou .env),
# nunca pelo codigo nem pela imagem.
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
