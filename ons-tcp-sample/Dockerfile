FROM openjdk:8
LABEL maintainer="YangLong<410357434@163.com>"
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/ons-tcp-sample.jar /app.jar
RUN mkdir -p /home/admin/logs/ons-http-sample
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]