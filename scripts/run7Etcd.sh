#!/bin/bash

# Remove current nodes
docker rm $(docker stop $(docker ps -a -q --filter name=etcd-node))

ETCD_VERSION=v3.0.0-beta.0
TOKEN=etcd-cluster-1
CLUSTER_STATE=new
NAME_1=etcd-node-0
NAME_2=etcd-node-1
NAME_3=etcd-node-2
NAME_4=etcd-node-3
NAME_5=etcd-node-4
NAME_6=etcd-node-5
NAME_7=etcd-node-6
P2P_1=localhost:2480
P2P_2=localhost:2580
P2P_3=localhost:2680
P2P_4=localhost:2780
P2P_5=localhost:2880
P2P_6=localhost:2980
P2P_7=localhost:3080
HOST_1=localhost:4101
HOST_2=localhost:4201
HOST_3=localhost:4301
HOST_4=localhost:4401
HOST_5=localhost:4501
HOST_6=localhost:4601
HOST_7=localhost:4701

CLUSTER=${NAME_1}=http://${P2P_1},${NAME_2}=http://${P2P_2},${NAME_3}=http://${P2P_3},${NAME_4}=http://${P2P_4},${NAME_5}=http://${P2P_5},${NAME_6}=http://${P2P_6},${NAME_7}=http://${P2P_7}

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

# For node 4 - @localhost:4401
THIS_NAME=${NAME_4}
THIS_PIP=${P2P_4}
THIS_HIP=${HOST_4}
docker run -d -p 2780:2780 -p 4401:4401 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}

# For node 5 - @localhost:4501
THIS_NAME=${NAME_5}
THIS_PIP=${P2P_5}
THIS_HIP=${HOST_5}
docker run -d -p 2880:2880 -p 4501:4501 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}

# For node 6 - @localhost:4601
THIS_NAME=${NAME_6}
THIS_PIP=${P2P_6}
THIS_HIP=${HOST_6}
docker run -d -p 2980:2980 -p 4601:4601 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}

# For node 7 - @localhost:4701
THIS_NAME=${NAME_7}
THIS_PIP=${P2P_7}
THIS_HIP=${HOST_7}
docker run -d -p 3080:3080 -p 4701:4701 --net=host --name ${THIS_NAME} quay.io/coreos/etcd:${ETCD_VERSION} \
    --name ${THIS_NAME} \
    --initial-advertise-peer-urls http://${THIS_PIP} --listen-peer-urls http://${THIS_PIP} \
    --advertise-client-urls http://${THIS_HIP} --listen-client-urls http://${THIS_HIP} \
    --initial-cluster ${CLUSTER} \
    --initial-cluster-state ${CLUSTER_STATE} --initial-cluster-token ${TOKEN}


sleep 2

curl http://localhost:4101/v2/keys/vhosts -XPUT -d dir=true

curl http://localhost:4101/v2/keys/tenants -XPUT -d dir=true
