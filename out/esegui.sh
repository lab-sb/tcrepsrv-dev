export JAVA_HOME=/usr/java/jdk1.8.0_201-amd64
#export JAVA_HOME=/usr/java/jdk1.8.0_211-i586
echo "Current NODEMST:"$NODEMST
echo "Please set desidered node in variable NODEMST=<node> to read config file in $RDBBDR/conf/<node>_rdb_bdr.conf"
read a

PASSED_ARGUMENTS=$@
echo "PASSED_ARGUMENTS:" $PASSED_ARGUMENTS

$JAVA_HOME/bin/java -cp ../lib/log4j-core-2.2.jar:../lib/log4j-api-2.2.jar:../lib/commons-cli-1.4.jar:../lib/postgresql-42.2.19.jar:../lib/pgjdbc/pgjdbc/src/:. $@


#current=`pwd`
#cd $RDBBDR/bin
#
#.  $RDBBDR/conf/RdbBdr_common.conf ; . $RDBBDR/conf/runJavaApplication.sh; runJREApplication -jar $RDBBDR/bin/rdbbdr_-cli.jar "$@"
#
#cd $current


