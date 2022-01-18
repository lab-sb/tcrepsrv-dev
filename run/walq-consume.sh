#!/bin/sh

ilpid=${1}
iltable=${2}
ntimes=${3:-5}
nsleep=${4:-1}

MyPID=$$
SRV=`hostname`

modulo="$1"


date
echo " Run the Loop walq : ${ilpid} in background process..."


count=0

while true
do
 echo "$( date ) : $( /usr/bin/time -f %E psql -p 5432 glftest -qAtX -f walq-scan.sql >> walq-scan.log 2>&1 )"

 count=`expr $count + 1`

#if [ $(( count % 1000 ))  -eq 0 ]
#then
#  echo "$( date ) : $( /usr/bin/time -f %E psql -p 5432 glftest -qAtX -f  walq-trunc.sql 2>&1 )"
#
#fi

done




#while true; do echo "$( date ) : $( /usr/bin/time -f %E psql -p 5432 glftest -qAtX -f walq-scan.sql 2>&1 )"; done
