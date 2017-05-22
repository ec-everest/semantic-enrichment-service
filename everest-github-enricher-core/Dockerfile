# Image builder of a rabbitmq installation with the management ui and the shovel plugin enable
# 1. to build this image run: docker build -t andresgs77/rabbitmq3:v0.1 pathTothisfile (is easier to go to the directory and use .)
# 2. to create a container based on this image run: docker run -d --hostname yourhostname --name yourrabbitname -p 15672:15672 -p 5672:5672 andresgs77/rabbitmq3:v0.1
# 3. To see the container id: docker ps
# 4. to get a interactive shell run:  docker exec -it containerId bash
# 5. Check your docker machine ip: docker-machine ip 
# 6. To connect to the management use: http://<docker-machine-ip>:15672 
FROM rabbitmq:3-management
MAINTAINER Andres Garcia <agarcia@expertsystem.com>
RUN echo 'Installing shoveling plugin'
RUN rabbitmq-plugins enable rabbitmq_shovel
