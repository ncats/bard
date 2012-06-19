BARD REST API
=============

This repository hosts the BARD REST API that provides query access to the backend BARD database. The API is
designed using the [Jersey ](http://jersey.java.net/) framework and is meant to be hosted in an application 
container. Currently, we have tested it in [Tomcat](http://tomcat.apache.org/) 6, but it should work in any other compliant application container
([GlassFish](http://en.wikipedia.org/wiki/GlassFish), [Jetty](http://en.wikipedia.org/wiki/Jetty_(web_server)) etc.)

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

Testing
-------

We are currently employing the [TestNG](http://testng.org/doc/index.html) framework for testing. At this stage we have a small
set of tests focusing on the various entity resources - most of them are currently stubs and need to be fleshed out. To run
the tests do
```
ant clean run-tests
```
and view `test.out/index.html` in your browser to view the summary.

Unit tests that focus on database functionality can be run via the `run-db-tests` target. These have been seperated out since
it requires a connection to a MySQL database, which developers may not have installed. If you choose to run these tests
you should place a file called `database.parameters` in the root directory of the distribution. The contents of the file
should be something like
```
jdbc:mysql://db.host.name:port/schema,username,password
```
As with the general tests, the results can be viewed in `test.out/index.html`

There is a special test target called `rest-heartbeat` that simply retrieves the `/_info` resource for all available
entities on the main development server and checks that the HTTP status code is 200. While this does not check that the resource is returning the
correct values, it does indicate that the resources are available. Ideally, there should be no failures for this
test target. If there are failures, that will indicate that one or more resources are not available.

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

The REST API also supports the concept of plugins - user contributed Java classes that appear as resources in the REST hierarchy. See
[`plugins/`](https://github.com/ncatsdpiprobedev/bard/tree/master/src/gov/nih/ncgc/bard/plugin) for some examples. A more detailed description of how plugins should be written can be found [here](https://github.com/ncatsdpiprobedev/bard/wiki/Plugins). Currently the example plugins are built as part of the WAR build target. However, users can provide a standalone JAR file containing
the components of their plugin. The actual deployment mechanism of such code is still to be decided. The tools directory also contains a plugin 
validator class. This can be used within client code to ensure a valid plugin. In the future this will be converted to an Ant task to allow 
validation at build time.
