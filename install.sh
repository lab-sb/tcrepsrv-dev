#!/bin/sh
#
# ----------------------------------------------------------------------------
# --
# -- Copyright (c) 2018-2022  Silvio Brandani <support@tcapture.net>. All rights reserved.
# --
# ----------------------------------------------------------------------------

# ---------------------------------------------------------------------
# installation script.
# ---------------------------------------------------------------------
#

# Functions

#
# variableInFile()
#

variableInFile () {
    variable=${1}
    file=${2}

    . ${file}
    eval value=\$\{${variable}\}
    echo ${value}
}


#
# print_usage()
#

print_usage() {

echo ""
echo "Usage: install.sh"  
echo ""
}

#
# message()
#

#
# message()
#
message()
{
        #echo $*
         echo -e  ${Yellow}--$Color_Off - ${Green}${*}$Color_Off 2>&1
}

message_date()
{
        #echo $*
         echo -e  ${Yellow}$(date)$Color_Off - ${Green}${*}$Color_Off 2>&1
}

title()
{
        #echo $*
         echo -e  ${Red}${*}$Color_Off 2>&1
}

subtitle()
{
        #echo $*
         echo -e  ${Purple}${*}$Color_Off 2>&1
}


#
# Environment
#
MyPID=$$
SRV=`hostname`
isok=0

# Reset
Color_Off='\e[0m'       # Text Reset

# Regular Colors
Black='\e[0;30m'        # Nero
Red='\e[0;31m'          # Rosso
Green='\e[0;32m'        # Verde
Yellow='\e[0;33m'       # Giallo
Blue='\e[0;34m'         # Blu
Purple='\e[0;35m'       # Viola
Cyan='\e[0;36m'         # Ciano
White='\e[0;37m'        # Bianco



current=`pwd`

# Make sure only root can run our script
if [ "$(id -u)" != "0" ]; then
   message "This script must be run as root" 1>&2
   exit 1
fi

if [ ! -f TCVer ]; then
  message "Please execute install.sh under TCapture software folder (ie: ~/rdbbdr-0.9.6/ !"
  exit $STATE_UNKNOWN
fi


echo "export RDBBDR=${current}" > .rdbbdr_env.sh

#Source it

. ${current}/.rdbbdr_env.sh



if [ -z "$RDBBDR" ] ; then
  message "RDBBDR HOME not set !  Should be set in .rdbbdr_env.sh"
  print_usage
  exit $STATE_UNKNOWN
fi


if [ ! -f /usr/pgsql-10/lib/rdblogdec.so  ]; then 
	subtitle " installing TCapture logical decoding library under /usr/pgsql-10/lib"
	cp lib/rdb_decoder/rdblogdec.so /usr/pgsql-10/lib/
else
	 message "rdblogdec.so already exists under /usr/pgsql-10/lib"
fi



