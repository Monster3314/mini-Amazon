FROM ubuntu:18.04

RUN apt-get update && apt-get install -yq --no-install-recommends openjdk-17-jdk && apt-get clean
RUN apt-get update && apt-get install -yq --no-install-recommends gradle && apt-get clean

RUN mkdir /backend
WORKDIR /backend
ADD . /backend
#ENTRYPOINT [ "java","-jar","app/build/libs/app.jar" ]