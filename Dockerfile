FROM clojure:openjdk-11-tools-deps

# http-kit listens on port 8080
EXPOSE 8080

# Copy exactly what we need into the image
COPY deploy /app/deploy
COPY env /app/env
COPY resources /app/resources
COPY src /app/src
COPY deps.edn /app/deps.edn
COPY package.json /app/package.json
COPY shadow-cljs.edn /app/shadow-cljs.edn
COPY yarn.lock /app/yarn.lock

# Package the application as an uberjar
RUN /app/deploy/package.sh

# Run the uberjar
CMD "java" "-cp" "/app/target/rtc.jar" "clojure.main" "-m" "rtc.app"