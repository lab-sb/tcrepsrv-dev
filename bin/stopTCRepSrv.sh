# ----------------------------------------------------------------------------
# --
# -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
# --
# ----------------------------------------------------------------------------
NODEMST=""
current=`pwd`

PASSED_ARGUMENTS=$@

while [[ $# -gt 0 ]]
do
  i="$1"
    case $i in
      -n|--node)
       NODEMST="$2"
       shift # past argument
       shift # past value
      ;;
      *)    # unknown option
       shift # do nothing
      ;;
    esac
done

if [ -z "$RDBBDR" ]; then
    echo "RDBBDR must be set"
    exit -1
fi


if [ -z "$NODEMST" ]; then
#    NODEMST=`cat $RDBBDR/.rdbbdr_env| grep "NODEMST" | cut -f2 -d"="`
    echo "NODEMST not specify, use runTCRepSrv.sh -n nodemst"
    exit 0
fi

#isRunning=$(ps -ef|grep java|grep com.edslab.TCRepSrv|grep $PASSED_ARGUMENTS|grep -v grep|wc -l)
isRunning=$(ps -aef --forest|grep -v grep|grep -v  statusTCRepSrv |grep "TCRepSrv -n ${NODEMST}"|wc -l)



if [ $isRunning -eq 0 ]
	then 
	echo " Node " $NODEMST " not running"
	exit 0
fi

nohup  sh $RDBBDR/bin/TC_srvctl.sh --shutdown --node $NODEMST > /tmp/_shuttcrepsrv_$NODEMST 2>&1 &
sleep 3 
itera=0

while [ $(tail -1 /tmp/_shuttcrepsrv_$NODEMST| grep "Shutting down"|wc -l) -gt 0 ]
do
	echo "Still Shutting down.."
	sleep 1
	 itera=$(expr $itera + 1)
	if [ $itera -gt 10 ]
	then
		echo "Graceful shutdown exceed time limit, going to kill TC Replication Server process"
		ps -ef|grep java|grep com.edslab.TCRepSrv|grep "TCRepSrv -n ${NODEMST}"|grep -v grep |awk '{print "kill -9 " $2}'|sh
		if [ $(ps -ef|grep java|grep com.edslab.TCRepSrv|grep "TCRepSrv -n ${NODEMST}"|grep -v grep| wc -l) -eq 0 ]
		then
			echo "TC Replication Server process killed !!"
		fi
	fi
done


echo " Node " $NODEMST " shutdown !!"
exit 0


