FROM openjdk:11
VOLUME /tmp
EXPOSE 8002
ADD ./target/items-service-0.0.1-SNAPSHOT.jar items-service.jar
ENTRYPOINT ["java","-jar","items-service.jar"]