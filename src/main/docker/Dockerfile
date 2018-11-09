FROM centos:7.5.1804

RUN rpm -ivh https://www.foundationdb.org/downloads/5.2.5/rhel7/installers/foundationdb-clients-5.2.5-1.el7.x86_64.rpm \
		&& yum install -y wget \
		&& wget -qO- https://www.foundationdb.org/downloads/5.2.5/bindings/python/foundationdb-5.2.5.tar.gz | tar xvz -C /tmp \
        && cd /tmp/foundationdb-5.2.5/ && python /tmp/foundationdb-5.2.5/setup.py install

RUN mkdir /opt/fdb-clean -p 

COPY python/clean_tx_fdb.py /opt/fdb-clean/clean_tx_fdb.py

RUN chmod +x /opt/fdb-clean/clean_tx_fdb.py