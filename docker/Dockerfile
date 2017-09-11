FROM fedora:23

MAINTAINER "Martin Sivak" <msivak@redhat.com>

RUN mkdir -p /opt/bugautomation /opt/prometheus /opt/grafana /var/lib/prometheus /var/lib/grafana /etc/bugautomation

# Prometheus does not work properly when procps / iproute is missing
RUN yum install -y java-1.8.0-openjdk-headless tar procps iproute

RUN curl -L https://github.com/prometheus/prometheus/releases/download/v1.5.0/prometheus-1.5.0.linux-amd64.tar.gz >/prometheus.tar.gz && curl -L https://grafanarel.s3.amazonaws.com/builds/grafana-4.1.0-1484127817.linux-x64.tar.gz >/grafana.tar.gz && cd /opt/grafana && tar xvzf /grafana.tar.gz && cd /opt/prometheus && tar xvzf /prometheus.tar.gz && rm -rf /*.tar.gz

VOLUME /var/lib/prometheus /var/lib/grafana

ADD prometheus.yml /opt/bugautomation/
ADD run.sh /opt/bugautomation/
ADD server-*.tar.gz /opt/bugautomation/

EXPOSE 8080 3000

# Make sure you mount a dir containing config.properties to /etc/bugautomation

ENTRYPOINT /opt/bugautomation/run.sh

