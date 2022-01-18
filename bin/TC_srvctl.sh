# ----------------------------------------------------------------------------
# --
# -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
# --
# ----------------------------------------------------------------------------
current=`pwd`

PASSED_ARGUMENTS=$@


if [ -z "$RDBBDR" ]; then
    echo "RDBBDR must be set"
    exit -1
fi


echo "Launching.."

. $RDBBDR/etc/TCaptureRepSrv.config; . $RDBBDR/etc/runJavaApplication.sh; cd $RDBBDR/out ; runJREApplication -XX:-UsePerfData $JAVA_HEAP_SIZE -XX:ErrorFile=$RDBBDR/log/repserver_pid_%p.log -Djava.library.path=$RDBBDR/bin -Duser.timezone=UTC  -Djava.awt.headless=true  -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp $RDBBDR/lib/disruptor-3.3.0.jar:$RDBBDR/lib/log4j-core-2.2.jar:$RDBBDR/lib/log4j-api-2.2.jar:$RDBBDR/lib/postgresql-42.2.19.jar:$RDBBDR/lib/commons-cli-1.4.jar:.   com.edslab.TCSrvCTL $PASSED_ARGUMENTS



