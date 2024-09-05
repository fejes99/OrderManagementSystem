#!/usr/bin/env bash

mkdir microservices
cd microservices

curl https://start.spring.io/starter.tgz \
    -d bootVersion=3.3.1 \
    -d baseDir=product-service \
    -d type=gradle-project \
    -d javaVersion=21 \
    -d packaging=jar \
    -d name=product-service \
    -d packageName=se.david.microservices.core.product \
    -d groupId=se.david.microservices.core.product \
    -d artifactId=product-service \
    -d dependencies=actuator,webflux \
    -d version=1.0.0-SNAPSHOT | tar -xzvf -

curl https://start.spring.io/starter.tgz \
    -d bootVersion=3.3.1 \
    -d baseDir=inventory-service \
    -d type=gradle-project \
    -d javaVersion=21 \
    -d packaging=jar \
    -d name=inventory-service \
    -d packageName=se.david.microservices.core.inventory \
    -d groupId=se.david.microservices.core.inventory \
    -d artifactId=inventory-service \
    -d dependencies=actuator,webflux \
    -d version=1.0.0-SNAPSHOT | tar -xzvf -

curl https://start.spring.io/starter.tgz \
    -d bootVersion=3.3.1 \
    -d baseDir=order-service \
    -d type=gradle-project \
    -d javaVersion=21 \
    -d packaging=jar \
    -d name=order-service \
    -d packageName=se.david.microservices.core.order \
    -d groupId=se.david.microservices.core.order \
    -d artifactId=order-service \
    -d dependencies=actuator,webflux \
    -d version=1.0.0-SNAPSHOT | tar -xzvf -

curl https://start.spring.io/starter.tgz \
    -d bootVersion=3.3.1 \
    -d baseDir=shipping-service \
    -d type=gradle-project \
    -d javaVersion=21 \
    -d packaging=jar \
    -d name=shipping-service \
    -d packageName=se.david.microservices.core.shipping \
    -d groupId=se.david.microservices.core.shipping \
    -d artifactId=shipping-service \
    -d dependencies=actuator,webflux \
    -d version=1.0.0-SNAPSHOT | tar -xzvf -

curl https://start.spring.io/starter.tgz \
    -d bootVersion=3.3.1 \
    -d baseDir=order-composite-service \
    -d type=gradle-project \
    -d javaVersion=21 \
    -d packaging=jar \
    -d name=order-composite-service \
    -d packageName=se.david.microservices.composite.order \
    -d groupId=se.david.microservices.composite.order \
    -d artifactId=order-composite-service \
    -d dependencies=actuator,webflux \
    -d version=1.0.0-SNAPSHOT | tar -xzvf -

cd ..
