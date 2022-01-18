

export NODEMST=ita
nohup sh esegui.sh StartReplication  >> StartReplication_$NODEMST.log 2>&1 &


export NODEMST=usa

nohup sh esegui.sh StartReplication  >> StartReplication_$NODEMST.log 2>&1 &



