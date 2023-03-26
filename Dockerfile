FROM FROM java:8

RUN mkdir -p /tmp/gaps
RUN mkdir -p /tmp/gaps/lib
RUN mkdir -p /tmp/gaps/config


COPY ./lib/* /tmp/gaps/lib/
COPY ./config/* /tmp/gaps/config/
RUN cd /tmp/gaps
WORKDIR /tmp/gaps
RUN ls -lrt

EXPOSE 2525
ENTRYPOINT ["java","--enable-preview","-cp","/tmp/gaps/lib/gaps.jar:/tmp/gaps/lib/amqp-client-5.14.2.jar:/tmp/gaps/lib/commons-lang3-3.12.0.jar:/tmp/gaps/lib/javax.json-1.0.jar:/tmp/gaps/lib/json-smart-2.4.8.jar:/tmp/gaps/lib/slf4j.api-1.6.1.jar:/tmp/gaps/lib/asm-1.0.2.jar:/tmp/gaps/lib/commons-pool2-2.6.2.jar:/tmp/gaps/lib/jedis-2.8.1.jar:/tmp/gaps/lib/slf4j-simple-1.6.1.jar","-Xmx512m","com.http.httpStub"]
