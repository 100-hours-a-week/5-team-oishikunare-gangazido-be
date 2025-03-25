FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/*.jar app.jar

# JVM에 환경변수를 전달하도록 변경
CMD ["java", "-Dapp.pet.upload.dir=/tmp/uploads", "-jar", "app.jar"]
