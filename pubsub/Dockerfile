from ruby:2.6.0-alpine3.8
RUN apk update
RUN apk add ruby-dev gcc g++ make libc6-compat protobuf-c
RUN gem update --system
RUN gem install fluentd --no-document
#RUN gem install grpc --no-document
#RUN gem install google-cloud-pubsub --no-document
# RUN gem install fluent-plugin-gcloud-pubsub-custom --no-document
RUN gem install fluent-plugin-gcs -v "0.4.0.beta1" --no-document
ENTRYPOINT fluentd -c /etc/fluentd/fluentd.conf
