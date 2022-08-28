FROM openjdk:11 AS build
WORKDIR /usr/src/app
COPY . .
RUN ./gradlew init build -x test

FROM openjdk:11
COPY --from=build /usr/src/app/build/libs/haechi-backend-0.0.1-SNAPSHOT.jar /usr/app/haechi-backend.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/app/haechi-backend.jar"]

