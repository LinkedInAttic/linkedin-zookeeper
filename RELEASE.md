1.5.0 (2013/04/01)
------------------
* Fixed [#13](https://github.com/linkedin/linkedin-zookeeper/issues/13) _zk.sh get does not work_
* Implemented [#12](https://github.com/linkedin/linkedin-zookeeper/issues/12) _Upgrade to latest versions_
* Fixed [#11](https://github.com/linkedin/linkedin-zookeeper/issues/11) _set connect timeout to a more reasonable value than 100ms_
* Fixed [#10](https://github.com/linkedin/linkedin-zookeeper/issues/10) _Make zk.sh use $JAVA\_HOME_

This release uses the latest version of ZooKeeper (3.4.5). Thanks to Patrick Hunt for the pull request. The build now uses gradle 1.4.0 and can be invoked using the wrapper (``gradlew``) at the root.

Note that there is an [issue with ZooKeeper](https://issues.apache.org/jira/browse/ZOOKEEPER-1661) and the workaround for it is to use ``127.0.0.1`` instead of ``localhost``.

1.4.1 (2012/03/31)
------------------
* use of [linkedin-utils 1.8.0](https://github.com/linkedin/linkedin-utils/tree/v1.8.0)

1.4.0 (2011/09/23)
------------------
* use of [linkedin-utils 1.7.1](https://github.com/linkedin/linkedin-utils/tree/v1.7.1)
* Implemented [#1](https://github.com/linkedin/linkedin-zookeeper/issues/1) _Expose a getConnectString in the IZKClient interface and IZooKeeperFactory_ (thanks Hiram)

1.3.0 (2011/04/30)
------------------
* use of [linkedin-utils 1.4.0](https://github.com/linkedin/linkedin-utils/tree/v1.4.0)
* use of latest version of ZooKeeper (3.3.3)

1.2.2 (2011/01/26)
------------------
* made constructor public
* use of [linkedin-utils 1.3.0](https://github.com/linkedin/linkedin-utils/tree/v1.3.0)

1.2.1 (2010/12/20)
------------------
* use of `gradle-plugins 1.5.0` in order to support `gradle 0.9` (no version change as the code did not change)

1.2.1 (2010/12/07)
------------------
* use of [linkedin-utils 1.2.1](https://github.com/linkedin/linkedin-utils/tree/REL_1.2.1)

1.0.0 (2010/11/06)
------------------
* First release