# ----------------------------------------------------------------------------
# --
# -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
# --
# ----------------------------------------------------------------------------

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
    echo "NODEMST not specify, use runTCaptureRepSrv.sh -n nodemst"
    exit 0
fi

###RUNTC=$(ps -aef --forest|grep -v grep|grep -v $$|grep com.edslab.TCRepSrv|grep $PASSED_ARGUMENTS|wc -l)
RUNTC=$(ps -aef --forest|grep -v grep|grep -v $$|grep "com.edslab.TCRepSrv -n ${NODEMST}"|wc -l)

if [ $RUNTC -gt 0 ] ; then
	echo " TC Replication Server alredy running for node "$NODEMST
	exit -1
fi


FILE="$RDBBDR/conf/${NODEMST}_rdb_bdr.conf"
if ! [ -f "$FILE" ]; then
    echo "Configuration file $FILE for master node ${NODEMST} NOT exists."
	
   FILE2="$RDBBDR/conf/${NODEMST}_bdr_rdb.conf"
	if ! [ -f "$FILE2" ]; then
           echo "Configuration file $FILE2 for slave node ${NODEMST} NOT exists."
           exit -1
        else
	  echo "Configuration file $FILE2 for slave node ${NODEMST} exists, copying into $FILE "
	  cp $RDBBDR/conf/${NODEMST}_bdr_rdb.conf $RDBBDR/conf/${NODEMST}_rdb_bdr.conf
	fi
fi


echo "export NODEMST=$NODEMST" >  $RDBBDR/.current_master
. $RDBBDR/.current_master

cd $RDBBDR/bin

export LD_LIBRARY_PATH=$RDBBDR/lib:$LD_LIBRARY_PATH

ddd=$(date '+%Y-%m-%d-%H:%M:%S')

logfile=$(echo "TCapture_${NODEMST}_${ddd}.log")
errlogfile=$(echo "TCapture_${NODEMST}_${ddd}_err.log")
## echo "Logging startup messages to :" $logfile
echo "Logging exception  messages to :" $errlogfile

echo "Launching.."

. $RDBBDR/etc/TCaptureRepSrv.config; . $RDBBDR/etc/runJavaApplication.sh; cd $RDBBDR/out ; runJREApplication -XX:-UsePerfData $JAVA_HEAP_SIZE -XX:ErrorFile=$RDBBDR/log/repserver_pid_%p.log -Djava.library.path=$RDBBDR/bin -Duser.timezone=UTC  -Djava.awt.headless=true  -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp $RDBBDR/lib/disruptor-3.3.0.jar:$RDBBDR/lib/log4j-core-2.2.jar:$RDBBDR/lib/log4j-api-2.2.jar:$RDBBDR/lib/postgresql-42.2.19.jar:$RDBBDR/lib/commons-cli-1.4.jar:.   com.edslab.TCRepSrv $PASSED_ARGUMENTS  > /dev/null 2> $RDBBDR/log/$errlogfile &

#######  > /dev/null 2> $RDBBDR/log/$errlogfile &
#######>> $RDBBDR/log/$logfile 2> $RDBBDR/log/$errlogfile &  
#######>> $RDBBDR/log/$logfile 2>&1 &  

sleep 1
ps -aef --forest|grep -v grep |grep TCRepSrv|grep $PASSED_ARGUMENTS

echo "press any button to tail the log :"
read a

tail -99f $RDBBDR/log/TCRepSrv_$NODEMST.log

exit 0



