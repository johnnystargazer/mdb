ps -ef | grep java | grep fdb |  awk '{print $2}' | xargs kill
cd /tmp/mdb
git reset --hard 92ecc1102295625344b727a0f4b8178e46fcc0e5
git pull 
mvn clean compile assembly:single 
java -jar /tmp/mdb/target/*.jar > app.log 2>&1 &
