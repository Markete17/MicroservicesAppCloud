FROM openjdk:11
VOLUME /tmp
EXPOSE 9100
ADD ./target/oauth-server-0.0.1-SNAPSHOT.jar oauth-server.jar
ENTRYPOINT ["java","-jar","eureka-server.jar"]