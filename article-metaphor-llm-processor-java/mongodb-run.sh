#!/usr/bin/zsh

docker run -d \
    -p 27017:27017 \
    --name test-mongo \
    -e MONGO_INITDB_ROOT_USERNAME=metaphorai-db-user \
    -e MONGO_INITDB_ROOT_PASSWORD=metaphorai-db-password \
    mongo:8.0