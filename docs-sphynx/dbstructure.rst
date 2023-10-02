.. _dbstructure:

TCapture DB structure
=====================



TCapture RepSrv DB  
------------------


*       **Queue** table ``walq_<node>`` structure

        *       ``wid``         incremental sequence
        *       ``lsn``         Logical Sequence Numner
        *       ``xid``         transaction as is read from replication slot
        *       ``data``        text column containing DML
        *       ``dateop``      timestamp
        *       ``current_xid`` transaction xid of insert into queue of dml


