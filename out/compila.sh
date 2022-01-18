export JAVA_HOME=/usr/java/jdk1.8.0_201-amd64 
#export JAVA_HOME=/usr/java/jdk1.8.0_211-i586

#export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.222.b10-1.el7_7.x86_64/

$JAVA_HOME/bin/javac -cp ../lib/log4j-core-2.2.jar:../lib/log4j-api-2.2.jar:../lib/commons-cli-1.4.jar:../lib/postgresql-42.2.19.jar:../lib/pgjdbc/pgjdbc/src/:. $1
# javac -cp ../lib/postgresql-42.2.19.jar:../lib/pgjdbc/pgjdbc/src/:. $1
