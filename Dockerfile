FROM clojure:openjdk-11-tools-deps AS build
WORKDIR /app

COPY deps.edn deps.edn
COPY .git .git
COPY shadow-cljs.edn shadow-cljs.edn
COPY src src
COPY resources resources

RUN clojure -X:build uberjar

CMD java -cp target/rtc.jar clojure.main -m prod
