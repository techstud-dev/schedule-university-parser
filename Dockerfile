FROM confluentinc/cp-kafka:latest

COPY src/test/resources/* /etc/kafka/

WORKDIR /etc/kafka/

CMD ["kafka-server-start", "/etc/kafka/server.properties"]
