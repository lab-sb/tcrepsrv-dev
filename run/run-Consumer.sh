

nohup sh StartConsumer.sh mila nyci &
nohup sh StartConsumer.sh mila lond &

tail -f walq-scan_mila.log 
