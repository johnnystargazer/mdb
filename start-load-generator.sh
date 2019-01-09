#!/bin/bash
OPT="-Xms256m -Xms256m"
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 0 > /tmp/client-0.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 1 > /tmp/client-1.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 2 > /tmp/client-2.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 3 > /tmp/client-3.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 4 > /tmp/client-4.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 5 > /tmp/client-5.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 6 > /tmp/client-6.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 7 > /tmp/client-7.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 8 > /tmp/client-8.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 9 > /tmp/client-9.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 10 > /tmp/client-10.log 2>&1 &
java $OPT -jar /tmp/mdb/target/*.jar  10.42.12.205 11 > /tmp/client-11.log 2>&1 &
