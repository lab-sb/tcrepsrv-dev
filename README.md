# tcapture_dev

TCapture - Postgresql Multi-Master Database Replication Server

DESCRIPTION:
------------

This is version TCapture Replication Server Dev Edition Beta 1.0

COPYRIGHT:
----------

    Copyright (c) 2019-2022  silvio.brandani <mktg.tcapture@gmail.com>. All rights reserved.


REQUIREMENTS:
-------------
	Platforms: CentOS, 64-bit - Red Hat Enterprise Linux (RHEL), 64-bit
	Database: The database versions that can be managed by TCapture Replication Server as a producer or consumer databases are the following:  PostgreSQL versions 9.6,10, and 11
	Software: Java Runtime Environment (JRE) version 1.8. Any Java product such as Oracle® Java or OpenJDK may be used.

	

INSTALLATION:
-------------

To install this module type the following:

	git clone https://github.com/lab-sb/tcapture_dev.git

	as root:
		execute install.sh under TCapture software folder (cd tcapture_dev  directory)
		./install.sh  #cover the following steps:

			- set variable RDBBDR HOME in .rdbbdr_env.sh
			- soruce environment file .rdbbdr_env.sh
			- installing TCapture logical decoding library under /usr/pgsql-10/lib
			
			examples:
			
			1- echo "export RDBBDR=/var/lib/pgsql/scripts/mycode/rdbbdr" > .rdbbdr_env.sh
			2-. .rdbbdr_env.sh
			3-echo $RDBBDR
			4-/var/lib/pgsql/scripts/mycode/rdbbdr
			5-cd $RDBBDR
			6-cp ./rdbbdr/lib/rdb_decoder/rdblogdec.so /usr/pgsql-10/lib/


DEVELOPMENT:
------------

To follow or participate in the development of TCapture, use:

	git clone git://github.com/lab-sb/tcapture_dev.git

GETTING HELP:
-------------

For general questions and troubleshooting, please use mktg.tcapture@gmail.com
