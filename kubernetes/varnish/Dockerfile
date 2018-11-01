FROM debian:stretch

RUN apt-get -y update
RUN apt-get -y install debian-archive-keyring
RUN apt-get -y install curl gnupg apt-transport-https
RUN curl -L https://packagecloud.io/varnishcache/varnish61/gpgkey | apt-key add -

COPY sources.list /etc/apt/sources.list.d/varnishcache_varnish61.list

RUN apt-get -y update

RUN apt-get -y install varnish

# VOLUME ["/var/lib/varnish", "/etc/varnish"]
EXPOSE 80

ENTRYPOINT ["/usr/sbin/varnishd"]
