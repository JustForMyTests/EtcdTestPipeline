#!/bin/bash

docker start $(docker ps -a -q --filter name=etcd)
