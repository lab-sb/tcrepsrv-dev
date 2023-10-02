
# --------------------------------------------------------------------------------------------
# --
# -- Copyright (c) 2022-2023  Silvio Brandani <mktg.tcapture@gmail.com>. All rights reserved.
# --
# --------------------------------------------------------------------------------------------

NODEMST=""
current=`pwd`

PASSED_ARGUMENTS=$@

echo $PASSED_ARGUMENTS

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

echo "Listing running TCRepSrv program for ${NODEMST}.." 
#ps -aef --forest|grep -v grep|grep -v  statusTCRepSrv |grep TCRepSrv|grep "\-n $PASSED_ARGUMENTS"
	ps -aef --forest|grep -v grep|grep -v  statusTCRepSrv |grep "TCRepSrv -n ${NODEMST}"
sleep 2
echo "Listing running TCRepSrv program for all.." 
ps -aef --forest|grep -v grep|grep -v  statusTCRepSrv |grep TCRepSrv
