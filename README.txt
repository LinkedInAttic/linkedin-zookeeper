1. Introduction
---------------
The project represents a set of utility classes and wrappers around ZooKeeper 
(http://hadoop.apache.org/zookeeper).

2. Compilation
--------------
In order to compile the code you need
* java 1.6
* gradle 0.9-rc2 (http://www.gradle.org/)

At the top simply run

gradle test

which should compile and run all the tests.

3. IDE Support
--------------
You can issue the command (at the top)

gradle idea

which will use the gradle IDEA plugin to create the right set of modules in order to open the
project in IntelliJ IDEA.

4. Directory structure
----------------------
* org.linkedin.zookeeper-impl
Contains a set of utility classes and wrappers to make it easier to use ZooKeeper:
- IZooKeeper is an interface/abstraction to ZooKeeper (which is (unfortunately) a class)
- IZKClient (which extends from IZooKeeper) adds a host of convenient calls and lifecycle listeners
- ZooKeeperURLHandler is a URL handler which knows how to handle zookeeper:/a/b/c type urls
- ZooKeeperTreeTracker (the core of this project) essentially keeps an in memory replica of a portion
of a tree or entire subtree with easy to use listeners (NodeEventsListener and ErrorListener): you get
notified when nodes are added, updated or deleted (you never deal with ZooKeeper watchers, nor have 
to set them over and over!). You can see a good example of how to use this class in the glu project
(org.linkedin.glu.agent.tracker.AgentsTrackerImpl.groovy)
- StandaloneZooKeeperServer: a simple class to start a standalone ZooKeeper server (simple to use
in testing)

Javadoc: http://www.kiwidoc.com/java/l/p/org.linkedin/org.linkedin.zookeeper-impl

* org.linkedin.zookeeper-cli
A command line (similar to the one bundled with ZooKeeper) with the idea of having a syntax very close
to a 'regular' shell:
zk.sh ls /a/b/c
zk.sh du /a/b/d
zk.sh put localFile.txt /a/b/c
zk.sh cat /a/b/c
etc...

* org.linkedin.zookeeper-server
Simply create a packaged server which is easy to install and start. Useful in dev.

5. Installing/Running locally
-----------------------------
To install the zookeeper server:
cd org.linkedin.zookeeper-server
gradle package-install

then go to the install directory and run 
./bin/zkServer start

To install the zookeeper cli:
cd org.linkedin.zookeeper-cli
gradle package-install

then go to the install directory and run (to see help)
./bin/zk.sh -h

and then try

./bin/zk.sh ls /

which returns

zookeeper

Note: it should work on any Linux/Unix based system (developped/tested on Mac OS X)

6. Build configuration
----------------------
You can configure some of the build properties by creating the file
~/.org.linkedin.linkedin-zookeeper.properties

Properties:
# where the build goes (if you don't like to have it in the source tree)
top.build.dir=xxx
# where the software is installed (when running package-install)
top.install.dir=xxx
# where the software is published (locally) (when running uploadArchives)
top.publish.dir=xxx

(see build.gradle for how it is being used)