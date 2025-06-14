# The base image
FROM ubuntu:20.04

# Prevent interactive prompts during installation
ENV DEBIAN_FRONTEND=noninteractive

# Initialize system tools
RUN apt-get update && apt-get install -y \
    iputils-ping \
    wget \
    gnupg \
    openjdk-17-jdk \
    maven \
    python3 \ 
    python3-pip \
    mysql-server \
    mysql-client \
    git \
    curl \
    libssl-dev \
    build-essential \
    net-tools \
    sudo \
    lsof \
    iproute2 \
    tar

# Set JAVA_HOME globally
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Verify Java installation
RUN java -version && echo $JAVA_HOME

# Download & Extract CloudStack Source Code
RUN mkdir -p /cloudstack && \
    wget https://dlcdn.apache.org/cloudstack/releases/4.20.0.0/apache-cloudstack-4.20.0.0-src.tar.bz2 -P /tmp && \
    tar -xjf /tmp/apache-cloudstack-4.20.0.0-src.tar.bz2 -C /cloudstack

# Set working directory
WORKDIR /cloudstack/apache-cloudstack-4.20.0.0-src

# Build from source
RUN mvn clean install -P developer,systemvm

# Ensure MySQL user has a valid shell before installation
RUN usermod -s /bin/bash mysql

# Create a MySQL data directory volume
VOLUME /var/lib/mysql

# Copy entrypoint and init scripts into CloudStack directory
COPY docker-entrypoint.sh /cloudstack/docker-entrypoint.sh
COPY init-mysql.sh /cloudstack/init-mysql.sh
RUN chmod +x /cloudstack/docker-entrypoint.sh /cloudstack/init-mysql.sh

# Expose CloudStack and MySQL ports
EXPOSE 8080 3306

# Keep the container running for debugging
CMD ["tail", "-f", "/dev/null"]
