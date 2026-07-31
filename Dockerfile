FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx512m"

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul"

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=builder --chown=spring:spring /workspace/build/libs/app.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
