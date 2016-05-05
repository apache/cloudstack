FROM ubuntu:16.04

ENV DEBIAN_FRONTEND noninteractive

COPY sources.list /etc/apt/sources.list

RUN apt-get update && \
    apt-get install -y \
        build-essential \
        vim \
        curl \
        mysql-client \
        mysql-server \
        libxml2-utils \
        locales-all \
        python-apt \
        wget \
        g++ \
        make \
        ruby \
        ruby-dev \
        openjdk-8-jdk \
        git \
        maven && \
    gem install fpm