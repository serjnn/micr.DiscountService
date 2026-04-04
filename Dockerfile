FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/DiscountService-0.0.1-SNAPSHOT.jar /app/discount-service.jar
EXPOSE 7005
ENTRYPOINT ["java", "-jar", "/app/discount-service.jar"]
