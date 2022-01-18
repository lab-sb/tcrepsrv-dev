ps -ef|grep onsumer|grep -v grep |awk '{print "kill "$2}'|sh
