#!/bin/sh -x
if [ ! -r /etc/bugautomation/config.properties ]; then
  echo "Please configure the tool by placing config.properties to /etc/bugautomation"
  exit 1
fi

/opt/prometheus/prometheus*/prometheus -config.file=/opt/bugautomation/prometheus.yml -storage.local.path=/var/lib/prometheus &
PROMETHEUS=$!

pushd /opt/grafana/grafana*
GF_PATHS_DATA=/var/lib/grafana ./bin/grafana-server web &
GRAFANA=$!
popd

java -Dbug.config=/etc/bugautomation/config.properties -jar /opt/bugautomation/server-*/*.jar &
BA=$!

_term() { 
  echo "Caught SIGTERM signal!" 
  kill -TERM "$BA" 2>/dev/null
}

trap _term SIGTERM

wait $BA

kill $GRAFANA
kill $PROMETHEUS

wait $GRAFANA
wait $PROMETHEUS

