.. _cleanup:

TCapture DB structure CleanUp 
=============================



.. code-block:: sh


  sh TC_srvctl.sh --unset  --node swap --type consumer --producer qas --force
   sh TC_srvctl.sh --unset  --node qas  --type consumer --producer swap --force


   sh TC_srvctl.sh --unset  --node qas  --type producer --force
   sh TC_srvctl.sh --unset  --node swap --type producer --force
   sh TC_srvctl.sh --unset  --node swap --type consumer --producer qas --force
   sh TC_srvctl.sh --unset  --node qas  --type consumer --producer swap --force

on eache primary database:



.. code-block:: sh


 drop schema _rdb_bdr cascade;
 drop schema _rdb_bdr cascade;



TCapture RepSrv Software Cleanup
--------------------------------

.. code-block:: sh


	$RDBBDR\cleanup.sh

	rm -Rf $RDBBDR



