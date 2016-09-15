#!/bin/bash

CYAN='\033[1;36m'
NC='\033[0m'

echo "${CYAN}Remaking 'vhosts' and 'tenants' directories for database at $* ...${NC}"

for var in "$@"
do
  curl -L -X DELETE http://$var/v2/keys/vhosts?recursive=true
  curl -L -X DELETE http://$var/v2/keys/tenants?recursive=true
  curl -L -X PUT http://$var/v2/keys/vhosts -d dir=true
  curl -L -X PUT http://$var/v2/keys/tenants -d dir=true
done

