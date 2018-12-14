#
# Ubuntu Dockerfile
#
# https://github.com/dockerfile/ubuntu
#

# Pull base image.
FROM ubuntu:14.04

# Install.
RUN \
	apt-get -y update && \ 
	apt-get -y install git-core curl wget software-properties-common && \
	echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
    add-apt-repository -y ppa:webupd8team/java && \
	apt-get update && \
	apt-get install -y oracle-java8-installer && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/cache/oracle-jdk8-installer && \
	cd /tmp && \
	git clone https://github.com/johnnystargazer/mdb.git && \
    wget https://www-us.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz -O - | tar -xz && \
	export PATH=$PATH:/tmp/apache-maven-3.6.0/bin && \
	wget https://www.foundationdb.org/downloads/6.0.15/ubuntu/installers/foundationdb-clients_6.0.15-1_amd64.deb && \
	dpkg -i foundationdb-clients_6.0.15-1_amd64.deb && \
	rm -Rf /tmp/foundationdb-clients_6.0.15-1_amd64.deb
	
# Set environment variables.
ENV HOME /root


# Define working directory.
WORKDIR /root

ENV PATH "$PATH:/tmp/apache-maven-3.6.0/bin"


ENV FDB_CLUSTER hT4GheYX:LK4nCVQb@192.168.124.247:4500

Run	cd /tmp/mdb && mvn clean compile assembly:single 

EXPOSE 4567 8080

Run	echo "=== update 1 ===" &&  git config --global user.email "you@example.com" &&  git config --global user.name "Your Name" && cd /tmp/mdb && git pull && mvn clean compile assembly:single 

# Define default command.
CMD ["java","-jar","/tmp/mdb/target/mdb-0.0.1-SNAPSHOT-jar-with-dependencies.jar"]