#!/bin/bash

cd `dirname $0`

java -Djava.net.preferIPv4Stack=true \
     -Djava.util.logging.config.file=conf/logging.properties \
     -jar bin/org.apache.felix.main-${felix.main.version}.jar

#java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 \
#     -Djava.net.preferIPv4Stack=true \
#     -Djava.util.logging.config.file=conf/logging.properties \
#     -jar bin/org.apache.felix.main-${felix.main.version}.jar