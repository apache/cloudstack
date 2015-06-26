FROM ubuntu:14.04

RUN apt-get -y update && apt-get install -y \
    genisoimage \
    git \
    maven \
    openjdk-7-jdk \
    python-dev \
    python-setuptools \
    python-pip \
    supervisor

RUN echo 'mysql-server mysql-server/root_password password root' |  debconf-set-selections; \
    echo 'mysql-server mysql-server/root_password_again password root' |  debconf-set-selections;

RUN apt-get install -qqy mysql-server && \
    apt-get clean all

RUN (/usr/bin/mysqld_safe &); sleep 5; mysqladmin -u root -proot password ''

RUN pip install --allow-external mysql-connector-python mysql-connector-python

COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY . ./root
WORKDIR /root

RUN mvn -Pdeveloper -Dsimulator -DskipTests clean install

RUN (/usr/bin/mysqld_safe &); \
    sleep 3; \
    mvn -Pdeveloper -pl developer -Ddeploydb; \
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator; \
    pip install tools/marvin/dist/Marvin-4.5.2.tar.gz

EXPOSE 8080

CMD ["/usr/bin/supervisord"]
