BARD REST API
=============

This repository hosts the BARD REST API that provides query access to the backend BARD database. The API is
designed using the [Jersey ](http://jersey.java.net/) framework and is meant to be hosted in an application 
container. Currently, we have tested it in [Tomcat](http://tomcat.apache.org/) 6 and ([GlassFish](http://en.wikipedia.org/wiki/GlassFish), 
but it should work in any other compliant application container such as [Jetty](http://en.wikipedia.org/wiki/Jetty_(web_server)).

Dependencies
------------

The backend database currently runs on [MySQL](http://www.mysql.com/). The repository does not contain the MySQL ODBC [connector](http://www.mysql.com/products/connector/). Instead it should
be available within the gloabl classpath of the application container. In addition, some resources implemented by the current
code have a dependency on the ChemAxon jchem.jar file. Due to license restrictions this is not included in the repository. You
should obtain an appropriately licensed version and place it under `lib/`.


Building
--------

The REST API is deployed as a WAR file which can be generated using
```
ant war-rest
```
You can then copy `deploy/bard.war` to the application containers' webapp directory and restart the server (if required)

Tomcat Configuration
--------------------

The API code employs JDBC pooling and is currently using MySQL as the backend database. If you plan to host the API locally, you will
need an instance of the backend database as well. After that has been set up, ensure that you have the MySQL ODBC connector located in 
the global Tomcat classpath and then add the following entry to `config/context.xml` in the Tomcat home directory.
```
  <Resource
      name="jdbc/bard"
      scope="Shareable"
      type="javax.sql.DataSource"
      url="jdbc:mysql://data.base.server:portnum/dbname?zeroDateTimeBehavior=convertToNull" 
      driverClassName="com.mysql.jdbc.Driver"
      username="bard"
      password="bard"
      maxIdle="3"
      maxActive="20"
      removeAbandoned="true"
      removeAbandonedTimeout="60"
      testOnBorrow="true"
      validationQuery="select 1"
      logAbandoned="true"
      debug="99"/>
```

Plugins
-------

The REST API also supports the concept of plugins - user contributed Java classes that appear as resources in the REST hierarchy. See the 
[plugin repository](https://github.com/ncatsdpiprobedev/bardplugins) for some examples. A more detailed description of how plugins should be written can be found [here](https://github.com/ncatsdpiprobedev/bard/wiki/Plugins). Plugins are built & deployed separately from the main BARD API. A proxy server in front of all BARD resources performs the appropriate redirection
