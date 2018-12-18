ps -ef | grep java | grep mdb |  awk '{print $2}' | xargs kill
