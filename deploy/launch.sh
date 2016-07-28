#!/bin/bash
# first argument - digital ocean access token
# second argument - droplet name

docker-machine create --driver digitalocean --digitalocean-access-token=$1 --digitalocean-size=4gb --digitalocean-image ubuntu-16-04-x64 $2
eval $(docker-machine env $2)
docker-compose build
docker-compose up -d

