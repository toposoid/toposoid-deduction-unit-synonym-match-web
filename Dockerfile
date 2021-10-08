FROM toposoid/toposoid-core:0.1.0

WORKDIR /app

ENV DEPLOYMENT=local
ENV _JAVA_OPTIONS="-Xms2g -Xmx4g"

RUN git clone https://github.com/toposoid/toposoid-deduction-common.git \
&& cd toposoid-deduction-common \
&& sbt publishLocal \
&& rm -Rf ./target \
&& cd .. \
&& git clone https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web.git \
&& cd toposoid-deduction-unit-synonym-match-web \
&& sbt playUpdateSecret 1> /dev/null \
&& sbt dist \
&& cd /app/toposoid-deduction-unit-synonym-match-web/target/universal \
&& unzip -o toposoid-deduction-unit-synonym-match-web-0.1.0.zip


COPY ./docker-entrypoint.sh /app/
ENTRYPOINT ["/app/docker-entrypoint.sh"]

