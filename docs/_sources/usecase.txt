.. _usecase:

TCapture Use Cases
==================

Use cases
---------

-FULL DB WITH MASTER-MASTER REPLICATION

                Each DB Instance is supported by a single transactional Read/Write DB database that contains all Shipments even if only a subset of them can be edited (the others must be edited on the owning DB Instance).
                In this scenario we have to face a Multi Master replication even if, from a logical point of view, the replication remains Master-Slave (each record can be edited only in his own DB Instance) thus minimizing conflict issues.

-Migrating Document Storage Database with TCapture RepSrv

                • ~10 TB size of production database
                • Postgresql v9.5
                • Need to migrato to v10
                • A limited set of tables with binary data
                • A very big one with binary data
                document_data (doc_id integer,doc_data bytea )
                Migration difficulties
                • Time obstacle dump/restore
                • Pg_basebackup not possible between main versions
                • ??
                • Replication server between versions


