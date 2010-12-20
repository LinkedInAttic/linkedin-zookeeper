Introduction
============
The project represents a set of utility classes and wrappers around [ZooKeeper](http://hadoop.apache.org/zookeeper).

Download
========
You can download pre-built binaries directly from the [github downloads page](https://github.com/linkedin/linkedin-zookeeper/downloads)

* `org.linkedin.zookeeper-server-<version>.tgz`:
  A simple to use prepackaged zookeeper distribution:

        ./bin/zkServer.sh start

* `org.linkedin.zookeeper-cli-<version>.tgz`:
  A command line (similar to the one bundled with ZooKeeper) with the idea of having a syntax very close to a 'regular' shell:

          zk.sh ls /a/b/c
          zk.sh du /a/b/d
          zk.sh put localFile.txt /a/b/c
          zk.sh cat /a/b/c
          etc...

Compilation
===========
In order to compile the code you need

* java 1.6
* [gradle 0.9](http://www.gradle.org/)

At the top simply run

    gradle test

which should compile and run all the tests.

IDE Support
===========
You can issue the command (at the top)

    gradle idea

which will use the gradle IDEA plugin to create the right set of modules in order to open the
project in IntelliJ IDEA.

Directory structure
===================

* `org.linkedin.zookeeper-impl`:
Contains a set of utility classes and wrappers to make it easier to use ZooKeeper:

  * `IZooKeeper` is an interface/abstraction to ZooKeeper (which is (unfortunately) a class)
  * `IZKClient` (which extends from `IZooKeeper`) adds a host of convenient calls and lifecycle listeners
  * `ZooKeeperURLHandler` is a URL handler which knows how to handle `zookeeper:/a/b/c` type urls
  * `ZooKeeperTreeTracker` (the core of this project) essentially keeps an in memory replica of a portion
of a tree or entire subtree with easy to use listeners (`NodeEventsListener` and `ErrorListener`): you get notified when nodes are added, updated or deleted (you never deal with ZooKeeper watchers, nor have to set them over and over!). You can see a good example of how to use this class in the glu project [org.linkedin.glu.agent.tracker.AgentsTrackerImpl](https://github.com/linkedin/glu/blob/master/agent/org.linkedin.glu.agent-tracker/src/main/groovy/org/linkedin/glu/agent/tracker/AgentsTrackerImpl.groovy)
  * `StandaloneZooKeeperServer`: a simple class to start a standalone ZooKeeper server (simple to use
in testing)

Javadoc: [org.linkedin.zookeeper-impl](http://www.kiwidoc.com/java/l/p/org.linkedin/org.linkedin.zookeeper-impl)

* `org.linkedin.zookeeper-cli-impl`:
A command line (similar to the one bundled with ZooKeeper) with the idea of having a syntax very close to a 'regular' shell:

        zk.sh ls /a/b/c
        zk.sh du /a/b/d
        zk.sh put localFile.txt /a/b/c
        zk.sh cat /a/b/c
        etc...

* `org.linkedin.zookeeper-cli`:
Create the packaged version of the cli.

* `org.linkedin.zookeeper-server`:
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

Build configuration
===================
The project uses the [`org.linkedin.userConfig`](https://github.com/linkedin/gradle-plugins/blob/master/README.md) plugin and as such can be configured

    Example:
    ~/.userConfig.properties
    top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
    top.install.dir="/export/content/${userConfig.project.name}"
    top.release.dir="/export/content/repositories/release"
    top.publish.dir="/export/content/repositories/publish"
