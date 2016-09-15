#!/bin/bash

set -e

CYAN='\033[1;36m'
NC='\033[0m'

pause(){
 read -rp "" key 
}

echo "${CYAN}Checking if etcd database is available at http://localhost:4001/v2/keys/ ...\n${NC}"

#Display all directories and keys
curl http://localhost:4001/v2/keys?recursive=true | python -m json.tool

pause 
echo "${CYAN}Remaking /vhosts and /tenants directory ...\n${NC}"

curl -L -X DELETE http://localhost:4001/v2/keys/vhosts?recursive=true
curl -L -X DELETE http://localhost:4001/v2/keys/tenants?recursive=true
curl -L -X PUT http://localhost:4001/v2/keys/vhosts -d dir=true
curl -L -X PUT http://localhost:4001/v2/keys/tenants -d dir=true

echo "${CYAN}Ready to use the application at http://localhost:9080 using the suffix '/mgt/csfim/v1/tenants/'\n${NC}"
pause

#Test adding new mappings
echo "${CYAN}Testing creating completely new mappings of vhost to tenantID ...\n${NC}"
echo "${CYAN}Map testA.com:abcd ...\n${NC}"
echo "${CYAN}Map testB.com:efgh ...\n${NC}"
echo "${CYAN}Map testC.com:wxyz ...\n${NC}"
sleep 2
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/abcd/vhost/testA.com
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/efgh/vhost/testB.com
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/wxyz/vhost/testC.com
curl http://localhost:4001/v2/keys?recursive=true | python -m json.tool
pause

#Test adding new vhosts to existing tenantIDs
echo "${CYAN}Testing add vhost to existing tenantID ...\n${NC}"
echo "${CYAN}Map testD.com:abcd ...\n${NC}"
echo "${CYAN}Map testE.com:abcd ...\n${NC}"
echo "${CYAN}Map testF.com:wxyz ...\n${NC}"
sleep 2
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/abcd/vhost/testD.com
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/abcd/vhost/testE.com
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/wxyz/vhost/testF.com
curl http://localhost:4001/v2/keys?recursive=true | python -m json.tool
pause

#Test getting all vhosts of a tenantID
echo "${CYAN}Get all vhosts of tenantID: abcd ...\n${NC}"
sleep 2
curl -X GET http://localhost:9080/mgt/csfim/v1/tenants/abcd/vhost | python -m json.tool

#Test getting all vhosts
echo "${CYAN}Get all vhosts in the database ...\n${NC}"
sleep 2
curl -X GET http://localhost:9080/mgt/csfim/v1/tenants/vhost | python -m json.tool
pause

#Test updating mappings
echo "${CYAN}Testing updating current mappings ...\n${NC}"
echo "${CYAN}Update testA.com:abcd to testA.com:efgh ...\n${NC}"
echo "${CYAN}Update testC.com:wxyz to testC.com:efgh ... \n${NC}"
echo "${CYAN}Update testB.com:efgh to a new tenantID, ijkl ... \n${NC}"
sleep 2
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/efgh/vhost/testA.com
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/efgh/vhost/testC.com
curl -X PUT http://localhost:9080/mgt/csfim/v1/tenants/ijkl/vhost/testB.com
curl http://localhost:4001/v2/keys?recursive=true | python -m json.tool
pause

#Test get a mapping
echo "${CYAN}Testing to get the mapping from vhost:testC.com ...\n${NC}"
sleep 2
curl -X GET http://localhost:9080/mgt/csfim/v1/tenants/vhost/testC.com

#Test get all tenantIDs
echo "${CYAN}Testing to get all tenantIDs ...\n${NC}"
sleep 2
curl -X GET http://localhost:9080/mgt/csfim/v1/tenants | python -m json.tool
pause

#Test delete mappings
echo "${CYAN}Testing deletion of mappings ...\n${NC}"
echo "${CYAN}Delete testB.com:ijkl ...\n${NC}"
echo "${CYAN}Delete testC.com:efgh ...\n${NC}"
echo "${CYAN}Delete testD.com:abcd ...\n${NC}"
sleep 2
curl -X DELETE http://localhost:9080/mgt/csfim/v1/tenants/ijkl/vhost/testB.com
curl -X DELETE http://localhost:9080/mgt/csfim/v1/tenants/efgh/vhost/testC.com
curl -X DELETE http://localhost:9080/mgt/csfim/v1/tenants/abcd/vhost/testD.com
curl http://localhost:4001/v2/keys?recursive=true | python -m json.tool
pause

#Test delete tenantIDs
echo "${CYAN}Testing deletion of tenantIDs ...\n${NC}"
echo "${CYAN}Delete empty tenantID:ijkl ...\n${NC}"
echo "${CYAN}Delete tenantID:efgh which has a current mapping from testA.com ...\n${NC}"
sleep 2
curl -X DELETE http://localhost:9080/mgt/csfim/v1/tenants/ijkl
curl -X DELETE http://localhost:9080/mgt/csfim/v1/tenants/efgh
curl http://localhost:4001/v2/keys?recursive=true | python -m json.tool
pause

#Test get tenant details
echo 
echo "${CYAN}Testing get tenant details of tenantID:abcd ...\n${NC}"
sleep 2
curl -X GET http://localhost:9080/mgt/csfim/v1/tenants/abcd/account | python -m json.tool

















