kubectl exec -it locust-master -- locust -f /tmp/e.py   --master --no-web -c 12 --expect-slaves 12
kubectl exec -it locust-master -- locust -f locust_file.py   --master --no-web -c 1 --expect-slaves 1
