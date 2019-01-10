#!/bin/bash
OPT="-Xms256m -Xms256m"
i=0
end=$1
while [ $i -le $end ]; do
	echo "java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 $i > /tmp/client-$i.log 2>&1 &"
	java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 $i > /tmp/client-$i.log 2>&1 &
    i=$(($i+1))
done