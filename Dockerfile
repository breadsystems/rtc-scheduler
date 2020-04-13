FROM openjdk:8-stretch

COPY target/rtc.jar /app/rtc.jar
EXPOSE 8080

CMD "java" "-cp" "/app/rtc.jar" "clojure.main" "-m" "rtc.app"