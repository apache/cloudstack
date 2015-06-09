# Docker Files

Dockerfiles used to build CloudStack images available on Docker hub.


## Use images from docker.io

### CloudStack Management-server 

```
docker pull docker.io/cloudstack/management_centos6
docker run --name cloudstack -d -p 8080:8080 docker.io/cloudstack/management_centos6
```

### Marvin

Use marvin to deploy or test your CloudStack environment.
Use Marvin with cloudstack connection thru the API port (8096)

```
docker pull docker.io/cloudstack/marvin
docker run -ti --name marvin --link cloudstack:8096 docker.io/cloudstack/marvin /bin/bash
```


## How to build images

Image provide by docker.io/cloudstack are automatically build by Jenkins performing following tasks:


### CentOS 6

CentOS 6 image use RPM's from jenkins.buildacloud.org
tag:latest = master branch

1. build the base image

   ```
   docker build -f Dockerfile.centos6 -t docker.io/cloudstack/management_centos6 .
   ```

2. on jenkins, database and systemvm.iso are pre-deployed. the inital start require privileged container to
   mount systemvm.iso and copy ssh_rsa.pub into it.

   ```
   docker run --privileged -d --name cloudstack docker.io/cloudstack/management_centos6
   sleep 300
   docker stop cloudstack
   docker commit -m "init system.iso" -a "Apache CloudStack" cloudstack docker.io/cloudstack/management_centos6
   ```

### Marvin

Build Marvin container usable to deploy cloud in the CloudStack management server container.


1. to build the image

   ```
   docker build -f tools/docker/Dockerfile.marvin -t docker.io/cloudstack/marvin ../..
   ```
