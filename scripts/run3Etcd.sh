#!/bin/bash

# Remove current nodes
docker rm $(docker stop $(docker ps -a -q --filter name=etcd-node))

ETCD_VERSION=v3.0.0-beta.0
TOKEN=etcd-cluster-1
CLUSTER_STATE=new
NAME_1=etcd-node-0
NAME_2=etcd-node-1
NAME_3=etcd-node-2
P2P_1=localhost:2480
P2P_2=localhost:2580
P2P_3=localhost:2680
HOST_1=localhost:4101
HOST_2=localhost:4201
HOST_3=localhost:4301

CLUSTER=${NAME_1}=http://${P2P_1},${NAME_2}=http://${P2P_2},${NAME_3}=http://${P2P_3}

# For node 1 - @localhost:4101
THIS_NAME=${NAME_1}
THIS_PIP=${P2P_1}
THIS_HIP=${HOST_1}
docker run -d -p 2480:2480 -p 4101:4101 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}

# For node 2 - @localhost:4201
THIS_NAME=${NAME_2}
THIS_PIP=${P2P_2}
THIS_HIP=${HOST_2}
docker run -d -p 2580:2580 -p 4201:4201 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}

# For node 3 - @localhost:4301
THIS_NAME=${NAME_3}
THIS_PIP=${P2P_3}
THIS_HIP=${HOST_3}
docker run -d -p 2680:2680 -p 4301:4301 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}


sleep 2

curl http://localhost:4101/v2/keys/vhosts -XPUT -d dir=true

curl http://localhost:4101/v2/keys/tenants -XPUT -d dir=true


