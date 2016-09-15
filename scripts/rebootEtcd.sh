#!/bin/bash

docker rm $(docker stop $(docker ps -a -q --filter name=etcdSA))

docker run -d -p 4001:4001 -p 2380:2380 --net=host --name etcdSA quay.io/coreos/etcd:v3.0.0-beta.0 \
 -name etcdSA \
 -advertise-client-urls http://127.0.0.1:4001 \
 -listen-client-urls http://127.0.0.1:4001 \
 -initial-advertise-peer-urls http://127.0.0.1:2380 \
 -listen-peer-urls http://127.0.0.1:2380 \
 -initial-cluster-token etcd-cluster-SA \
 -initial-cluster etcdSA=http://127.0.0.1:2380

sleep 1.0

curl http://localhost:4001/v2/keys/vhosts -XPUT -d dir=true

curl http://localhost:4001/v2/keys/tenants -XPUT -d dir=true
