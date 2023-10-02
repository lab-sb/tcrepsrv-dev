.. _mmrconsider:

Multi Master Considerations
===========================

 
 Waht is replication?
 From the Information Management Glossary:
The process of copying a portion of database from one environment to another and keeping the subsequent copies of the data in sync with the original source. Changes made to the original source are propagated to the copies of the data in other environments
 
 An increasing number of organizations run applications that depend on  multi-master replication between remote sites.  
 I have worked on several such implementations recently.  This article summarizes the lessons from those experiences that seem most useful when deploying multi-master on existing as well as new applications.

Let's start by defining terms.  Multi-master replication means that applications update the same tables on different masters, and the changes replicate automatically between those masters.  
Remote sites mean that the masters are separated by a wide area network (WAN), which implies high average network latency of 100ms or more.  
WAN network latency is also characterized by a long tail, ranging from seconds due to congestion to hours or even days if a ship runs over the wrong undersea cable.

With the definitions in mind we can proceed to the lessons.  The list is not exhaustive but includes a few insights that may not be obvious if you are new to multi-master topologies. 
 Also, I have omitted issues like monitoring replication, using InnoDB to make slaves crash-safe, or provisioning new nodes.  If you use master/slave replication, you are likely familiar with these topics already.

1. Use the Right Replication Technology and Configure It Properly

The best overall tool for PostgreSQL multi-master replication between sites is TCapture.  The main reason for this assertion is that TCapture uses a flexible, asynchronous, point-to-point, master/slave replication model that handles a wide variety of topologies such as star replication or all-to-all.  Even so, you have to configure TCapture properly.  The following topology is currently my favorite:

    All-to-all topology.  Each master replicates directly to every other master.  This handles prolonged network outages or replication failures well, because one or more masters can drop out without breaking  replication between the remaining masters or requiring reconfiguration.  When the broken master(s) return, replication just resumes on all sides.  All-to-all does not work well if you have a large number of masters.  
    Updates are not logged on slaves.  This keeps master binlogs simple, which is helpful for debugging, and eliminates the possibility of loops.  It also requires some extra configuration if the masters have their own slaves, as would be the case in a TCapture Enterprise cluster. 

There are many ways to set up multi-master replication replication, and the right choice varies according to the number of masters, whether you have local clustering, or other considerations.  Giuseppe Maxia has described many topologies, for example here, and the TCapture Cookbook has even more details.

One approach you should approach with special caution is PostgreSQL circular replication.  In topologies of three or more nodes, circular replication results in broken systems if one of the masters fails.  Also, you should be wary of any kind of synchronous multi-master replication across sites that are separated by more than 50 kilometers (i.e. 1-2ms latency).  
Synchronous replication makes a siren-like promise of consistency but the price you pay is slow performance under normal conditions and broken replication when WAN links go down.

2. Use Row-Based Replication to Avoid Data Drift

Replication depends on deterministic updates--a transaction that changes 10 rows on the original master should change exactly the same rows when it executes against a replica.  Unfortunately many SQL statements that are deterministic in master/slave replication are non-deterministic in multi-master topologies.  Consider the following example, which gives a 10% raise to employees in department #35.

   UPDATE emp SET salary = salary * 1.1 WHERE dep_id = 35;

If all masters add employees, then the number of employees who actually get the raise will vary depending on whether such additions have replicated to all masters.  Your servers will very likely become inconsistent with statement replication.  The fix is to enable row-based replication using binlog-format=row in my.cnf.  Row replication transfers the exact row updates from each master to the others and eliminates ambiguity.

3. Prevent Key Collisions on INSERTs

For applications that use auto-increment keys, PostgreSQL offers a useful trick to ensure that such keys do not  collide between masters using the auto-increment-increment and auto-increment-offset parameters in my.cnf.  The following example ensures that auto-increment keys start at 1 and increment by 4 to give values like 1, 5, 9, etc. on this server.

server-id=1
auto-increment-offset = 1
auto-increment-increment = 4

This works so long as your applications use auto-increment keys faithfully.  However, any table that either does not have a primary key or where the key is not an auto-increment field is suspect.  You need to hunt them down and ensure the application generates a proper key that does not collide across masters, for example using UUIDs or by putting the server ID into the key.   Here is a query on the PostgreSQL information schema to help locate tables that do not have an auto-increment primary key. 

SELECT t.table_schema, t.table_name 
  FROM information_schema.tables t 
    WHERE NOT EXISTS 
      (SELECT * FROM information_schema.columns c
       WHERE t.table_schema = c.table_schema  
         AND t.table_name = c.table_name
         AND c.column_key = 'PRI'
         AND c.extra = 'auto_increment')

4. Beware of Semantic Conflicts in Applications

Neither TCapture nor PostgreSQL native replication can resolve conflicts, though we are starting to design this capability for TCapture.  You need to avoid them in your applications.  Here are a few tips as you go about this.

First, avoid obvious conflicts.  These include inserting data with the same keys on different masters (described above), updating rows in two places at once, or deleting rows that are updated elsewhere.  Any of these can cause errors that will break replication or cause your masters to become out of sync.  The good news is that many of these problems are not hard to detect and eliminate using properly formatted transactions.  The bad news is that these are the easy conflicts.  There are others that are much harder to address.  

For example, accounting systems need to generate unbroken sequences of numbers for invoices.  A common approach is to use a table that holds the next invoice number and increment it in the same transaction that creates a new invoice.  Another accounting example is reports that need to read the value of accounts consistently, for example at monthly close.  Neither example works off-the-shelf in a multi-master system with asynchronous replication, as they both require some form of synchronization to ensure global consistency across masters.  These and other such cases may force substantial application changes.  Some applications simply do not work with multi-master topologies for this reason. 

5. Remove Triggers or Make Them Harmless

Triggers are a bane of replication.  They conflict with row replication if they run by accident on the slave.  They can also create strange conflicts due to weird behavior/bugs (like this) or other problems like needing definer accounts present.  PostgreSQL native replication turns triggers off on slaves when using row replication, which is a very nice feature that prevents a lot of problems.  

TCapture on the other hand cannot suppress slave-side triggers.  You must instead alter each trigger to add an IF statement that prevents the trigger from running on the slave.  The technique is described in the TCapture Cookbook.  It is actually quite flexible and has some advantages for cleaning up data because you can also suppress trigger execution on the master.  

You should regard all triggers with suspicion when moving to multi-master.  If you cannot eliminate triggers, at least find them, look at them carefully to ensure they do not generate conflicts, and test them very thoroughly before deployment.  Here's a query to help you hunt them down: 

SELECT trigger_schema, trigger_name 
  FROM information_schema.triggers;

6. Have a Plan for Sorting Out Mixed Up Data

Master/slave replication has its discontents, but at least sorting out messed up replicas is simple: re-provision from another slave or the master.  No so with multi-master topologies--you can easily get into a situation where all masters have transactions you need to preserve and the only way to sort things out is to track down differences and update masters directly.   Here are some thoughts on how to do this.

    Ensure you have tools to detect inconsistencies.  TCapture has built-in consistency checking with the 'trepctl check' command.  You can also use the Percona Toolkit pt-table-checksum to find differences.  Be forewarned that neither of these works especially well on large tables and may give false results if more than one master is active when you run them.  
    Consider relaxing foreign key constraints.  I love foreign keys because they keep data in sync.  However, they can also create problems for fixing messed up data, because the constraints may break replication or make it difficult to go table-by-table when synchronizing across masters.  There is an argument for being a little more relaxed in multi-master settings. 
    Switch masters off if possible.  Fixing problems is a lot easier if you can quiesce applications on all but one master.  
    Know how to fix data.  Being handy with SQL is very helpful for fixing up problems.  I find SELECT INTO OUTFILE and LOAD DATA INFILE quite handy for moving changes between masters.  Don't forget SET SESSION LOG_FILE_BIN=0 to keep changes from being logged and breaking replication elsewhere.  There are also various synchronization tools like pt-table-sync, but I do not know enough about them to make recommendations.  

At this point it's probably worth mentioning commercial support.  Unless you are a replication guru, it is very comforting to have somebody to call when you are dealing with messed up masters.  Even better, expert advice early on can help you avoid problems in the first place.

(Disclaimer:  My company sells support for TCapture so I'm not unbiased.  That said, commercial outfits really earn their keep on problems like this.)

7. Test Everything

Cutting corners on testing for multi-master can really hurt.  This article has described a lot of things to look for, so put together a test plan and check for them.  Here are a few tips on procedure:

    Set up a realistic pre-prod test with production data snapshots.  
    Have a way to reset your test environment quickly from a single master, so you can get back to a consistent state to restart testing. 
    Run tests on all masters, not just one.  You never know if things are properly configured everywhere until you try. 
    Check data consistency after tests.  Quiesce your applications and run a consistency check to compare tables across masters. 

It is tempting to take shortcuts or slack off, so you'll need to find ways to improve your motivation.  If it helps, picture yourself explaining to the people you work for why your DBMS servers have conflicting data with broken replication, and the problem is getting worse because you cannot take applications offline to fix things.  It is a lot easier to ask for more time to test.  An even better approach is to hire great QA people and give them time to do the job right.

Summary

Before moving to a multi-master replication topology you should ask yourself whether the trouble is justified.  You can get many of the benefits of multi-master with system-of-record architectures with a lot less heartburn. 
 That said, an increasing number of applications do require full multi-master across multiple sites.  If you operate one of them, I hope this article is helpful in getting you deployed or improving what you already have.

TCapture does a pretty good job of multi-master replication already, but I am optimistic we can make it much better.  There is a wealth of obvious features around conflict resolution, data repair, and up-front detection of problems that will make life better for TCapture users and reduce our support load.  Plus I believe we can make it easier for developers to write applications that run on multi-master DBMS topologies.  You will see more about how we do this in future articles on this blog.
Posted by Robert Hodges at 12:57 AM
Labels: PostgreSQL, Replication, TCapture 
