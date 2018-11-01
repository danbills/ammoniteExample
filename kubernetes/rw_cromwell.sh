kubectl create configmap rw --from-file=varnish-rw-cromwell-config.vcl --from-file=mysql-sa.json --from-file=cromwell-rw-reader.conf --from-file=cromwell-rw-worker.conf --from-file=cromwell-sa.json
