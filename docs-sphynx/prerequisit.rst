.. _prerequisit:

TCapture Prerequisites
===========================
This section describes the supported database server versions, the required supporting software, etc.

Supported Platforms and Database Versions 
-----------------------------------------

TCapture Replication Server can be used on the following platforms:
 CentOS 7.x, 64-bit
 Red Hat Enterprise Linux (RHEL) 7.x, 64-bit

The database versions that can be managed by TCapture Replication Server as a producer or consumer databases are the following:
 PostgreSQL versions 9.6,10, and 11

Required Software 
-----------------
The following component must be installed on any host on which any TCapture Replication Server component is to be installed:
 Java Runtime Environment (JRE) version 1.8. Any Java product such as Oracle® Java or OpenJDK may be used.

System Resource Requirements 
----------------------------
The resource requirements for TC Replication Server are given below:

RAM
---
 A single instance of TCapture Replication Server requires a minimum of 6GB free memory available on the host OS. The core TCapture Replication Server process reserves 4GB for its heap usage.
 In the production environment, it is recommended to spare 12 to 16 GB of memory in order to make adjustments for a given workload.

CPU
---
 It is recommended to choose a modern system with multiple CPUs/cores

