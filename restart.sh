ps -ef | grep java | grep mdb |  awk '{print $2}' | xargs kill
cd /tmp/mdb
git reset --hard ffa902a030c03a3ff26d8ba4c210ac46442d1656
git pull 
mvn clean compile assembly:single 
java -jar /tmp/mdb/target/mdb-0.0.1-SNAPSHOT-jar-with-dependencies.jar > app.log 2>&1 &
