#!/bin/sh -x
if [ ! -r /etc/bugautomation/config.properties ]; then
  echo "Please configure the tool by placing config.properties to /etc/bugautomation"
  exit 1
fi

exec java -Dbug.config=/etc/bugautomation/config.properties -jar /opt/bugautomation/server-*/*.jar

