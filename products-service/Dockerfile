FROM openjdk:11
VOLUME /tmp
ADD ./target/products-service-0.0.1-SNAPSHOT.jar products-service.jar
ENTRYPOINT ["java","-jar","products-service.jar"]