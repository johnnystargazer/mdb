ps -ef | grep java | grep mdb |  awk '{print $2}' | xargs kill
cd /tmp/mdb
git reset --hard a1d4f1c8d347f1e54f509bfcc4247f17d3c6e7cf
git pull 
mvn clean compile assembly:single 
java -jar /tmp/mdb/target/mdb-0.0.1-SNAPSHOT-jar-with-dependencies.jar > app.log 2>&1 &
