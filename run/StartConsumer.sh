#!/bin/sh

slave=${1}
prim=${2}

pconf=${prim}_rdb_bdr.conf
sconf=${slave}_rdb_bdr.conf
MyPID=$$
SRV=`hostname`


variableInFile () {
    variable=${1}
    file=${2}

    . ${file}
    eval value=\$\{${variable}\}
    echo ${value}
}

echo "Read environment file : "$RDBBDR/.rdbbdr_env.sh

if [ ! -f $RDBBDR/.rdbbdr_env.sh ]; then
  echo "File $RDBBDR/.rdbbdr_env.sh  not found!"
  echo " Very Bad ! set RDBBDR variable to HOME of rdbbdr software"
  exit $STATE_UNKNOWN
fi


shost=$(variableInFile host $RDBBDR/conf/${sconf})
sport=$(variableInFile port $RDBBDR/conf/${sconf})
suser=$(variableInFile user $RDBBDR/conf/${sconf})
spwd=$(variableInFile pwd $RDBBDR/conf/${sconf})
sdb=$(variableInFile db $RDBBDR/conf/${sconf})
snode=$(variableInFile node $RDBBDR/conf/${sconf})



phost=$(variableInFile host $RDBBDR/conf/${pconf})
pport=$(variableInFile port $RDBBDR/conf/${pconf})
puser=$(variableInFile user $RDBBDR/conf/${pconf})
ppwd=$(variableInFile pwd $RDBBDR/conf/${pconf})
pdb=$(variableInFile db $RDBBDR/conf/${pconf})
pnode=$(variableInFile node $RDBBDR/conf/${pconf})

echo "Consumer - Primary $phost $pport $puser $ppwd $pdb !"
echo "Consumer - Slave $shost $sport $suser $spwd $sdb !"
read a

date
echo " Run the Loop walq : ${ilpid} in background process..."
 read a 

count=0

while true
do
  #echo "select runwalt_walq__${slave}(1000,1000);"| PGPASSWORD=${ppwd} psql -qt  -h ${phost} -p ${pport} -U ${puser} ${pdb}   >> walq-scan_${slave}_${pnode}.log 2>&1 
  echo "select runwalt_walq__${prim}(1000,1000);"| PGPASSWORD=${spwd} psql -qt  -h ${shost} -p ${sport} -U ${suser} ${sdb}   >> walq-scan_${slave}_${prim}.log 2>&1 
  sleep 1 

count=`expr $count + 1`

#if [ $(( count % 1000 ))  -eq 0 ]
#then
#  echo "$( date ) : $( /usr/bin/time -f %E psql -p 5432 glftest -qAtX -f  walq-trunc.sql 2>&1 )"
#
#fi

done




#while true; do echo "$( date ) : $( /usr/bin/time -f %E psql -p 5432 glftest -qAtX -f walq-scan.sql 2>&1 )"; done
