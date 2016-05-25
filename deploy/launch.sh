#!/bin/bash
# only one argument - digital ocean access token

docker-machine create --driver digitalocean --digitalocean-access-token=$1 --digitalocean-size=4gb colorize
eval $(docker-machine env colorize)
docker-compose build
docker-compose up -d

