FROM toposoid/toposoid-core:0.1.1

WORKDIR /app
ARG TARGET_BRANCH
ENV DEPLOYMENT=local
ENV _JAVA_OPTIONS="-Xms2g -Xmx4g"

RUN git clone https://github.com/toposoid/toposoid-deduction-common.git \
&& cd toposoid-deduction-common \
&& git fetch origin ${TARGET_BRANCH} \
&& git checkout ${TARGET_BRANCH} \
&& sbt publishLocal \
&& rm -Rf ./target \
&& cd .. \
&& git clone https://github.com/toposoid/toposoid-deduction-unit-synonym-match-web.git \
&& cd toposoid-deduction-unit-synonym-match-web \
&& git fetch origin ${TARGET_BRANCH} \
&& git checkout ${TARGET_BRANCH} \
&& sbt playUpdateSecret 1> /dev/null \
&& sbt dist \
&& cd /app/toposoid-deduction-unit-synonym-match-web/target/universal \
&& unzip -o toposoid-deduction-unit-synonym-match-web-0.1.1.zip


COPY ./docker-entrypoint.sh /app/
ENTRYPOINT ["/app/docker-entrypoint.sh"]

