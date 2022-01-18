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
    echo "NODEMST not specify, use runTWrapperSQL.sh -n nodemst"
    exit 0
fi

echo "export NODEMST=$NODEMST" >  $RDBBDR/.current_master
. $RDBBDR/.current_master

cd $RDBBDR/bin

export LD_LIBRARY_PATH=$RDBBDR/lib:$LD_LIBRARY_PATH

ddd=$(date '+%Y-%m-%d-%H:%M:%S')

logfile=$(echo "TWrapperSQL_${NODEMST}_${ddd}.log")
echo "Logging startup messages to :" $logfile

echo "Launching.."

. $RDBBDR/etc/TCaptureRepSrv.config; . $RDBBDR/etc/runJavaApplication.sh; cd $RDBBDR/out ; runJREApplication -XX:-UsePerfData $JAVA_HEAP_SIZE -XX:ErrorFile=$RDBBDR/log/repserver_pid_%p.log -Djava.library.path=$RDBBDR/bin -Duser.timezone=UTC  -Djava.awt.headless=true  -cp $RDBBDR/lib/postgresql-42.2.19.jar:$RDBBDR/lib/commons-cli-1.4.jar:.   com.edslab.TWrapperSQL $PASSED_ARGUMENTS  >> $logfile 2>&1 &


sleep 1
ps -aef --forest|grep -v grep |grep TWrapperSQL |grep $PASSED_ARGUMENTS

cat $logfile
#echo "press a to tail the log :"
#read a


#echo $(cat ../conf/${NODEMST}_log.conf|grep "^java.util.logging.FileHandler.pattern"|awk -F '=' '{print $2}' )
#echo " "
#cd $current
#tail -9f $(cat ../conf/${NODEMST}_log.conf|grep "^java.util.logging.FileHandler.pattern"|awk -F '=' '{print $2}')


