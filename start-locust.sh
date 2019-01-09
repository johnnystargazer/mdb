
locust -f /tmp/mdb/src/main/docker/server/locust/locust_file.py --master --no-web -c 12 --expect-slaves 12
