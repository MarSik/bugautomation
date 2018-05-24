FROM centos:centos7

MAINTAINER "Martin Sivak" <msivak@redhat.com>

RUN mkdir -p /opt/bugautomation /etc/bugautomation

RUN yum install -y java-1.8.0-openjdk-headless tar

ADD run.sh /opt/bugautomation/
ADD server-*.tar.gz /opt/bugautomation/

EXPOSE 8080

# Make sure you mount a dir containing config.properties to /etc/bugautomation

ENTRYPOINT /opt/bugautomation/run.sh

