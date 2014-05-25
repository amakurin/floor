#!/bin/bash

FLOOR_HOME="/home/user/cljr/floor16"
FLOOR_PIDS="tmp/pids"
FLOOR_JAR="target/floor16-0.1.0-SNAPSHOT-standalone.jar"

mkdir -p $FLOOR_HOME/$FLOOR_PIDS

java -Xms512m -Xmx1024m  \
-Dapi.url=/api -Dimg.server.url=http://192.168.1.106:8080/ \
-Ddefault.select.limit=20 \
-Dserver.side.js.path=$FLOOR_HOME/resources/public/js/ \
-Ddatabase="{:subprotocol \"mysql\" :subname \"//localhost/caterpillar\" :user \"caterpillar\" :password \"111111\" :delimiters \"\`\"}" \
-jar $FLOOR_HOME/$FLOOR_JAR 8090 &

echo $! > $FLOOR_HOME/$FLOOR_PIDS/floor16.pid
exit 0

