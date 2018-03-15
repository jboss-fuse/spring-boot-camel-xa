# Spring-Boot Camel XA Transactions Quickstart

This example demonstrates how to run a Camel Service on Spring-Boot that supports XA transactions on two external transactional resources: a JMS resource (ActiveMQ) and a database (PostgreSQL).

External resources can be provided by Openshift and must be started before running this quickstart.  

The quickstart uses Openshift `StatefulSet` resources to guarantee uniqueness of transaction managers and 
require a `PersistentVolume` to store transaction logs.

The application **supports scaling** on the `StatefulSet` resource. Each instance will have its own "in process" recovery manager.


All commands below requires one of these:
- be logged in to the targeted OpenShift instance (using oc login command line tool for instance)
- configure properties to specify to which OpenShift instance it should connect

IMPORTANT: This quickstart can run in 2 modes: standalone on your machine and on your Single-node OpenShift Cluster 

### Building

The example can be built with

    mvn clean install
    
### Running the Quickstart standalone on your machine

You can also run this booster as a standalone project directly.

Obtain the project and enter the project's directory.

Set the `database.*` and `broker.*` properties in `src/main/resources/application.properties` to point 
to running instances of a Postgresql database and a A-MQ broker.

Build the project:

    mvn clean package
    mvn spring-boot:run 

### Running the Quickstart on a Single-node OpenShift Cluster

If you have a single-node OpenShift cluster, such as Minishift or the Red Hat Container Development Kit, link:http://appdev.openshift.io/docs/minishift-installation.html[installed and running], you can also deploy your booster there. A single-node OpenShift cluster provides you with access to a cloud environment that is similar to a production environment.

To deploy your booster to a running single-node OpenShift cluster:

Log in and create your project:

    oc login -u developer -p developer
    oc new-project MY_PROJECT_NAME

Import base images in your newly created project (MY_PROJECT_NAME):

    oc import-image fis-java-openshift:2.0 --from=registry.access.redhat.com/jboss-fuse-6/fis-java-openshift:2.0 --confirm

Install dependencies:
- From the Openshift catalog install `postgresql` using `theuser` as username and `Thepassword1!` as password
- From the Openshift catalog install a `A-MQ` broker using `theuser` as username and `Thepassword1!` as password

Change the Postgresql database to accept prepared statements:

    
    oc env dc/postgresql POSTGRESQL_MAX_PREPARED_TRANSACTIONS=100


Create a persistent volume claim for the transaction log:

    oc create -f persistent-volume-claim.yml

Build and deploy your booster:

    mvn clean -DskipTests fabric8:deploy -Popenshift -Dfabric8.generator.fromMode=istag -Dfabric8.generator.from=MY_PROJECT_NAME/fis-java-openshift:2.0

### Using the Quickstart

Once the quickstart is running you can get the base service URL using the following command:


For Openshift:

    NARAYANA_HOST=$(oc get route spring-boot-camel-xa -o jsonpath={.spec.host})

For standalone installation:

    NARAYANA_HOST=localhost:8080

The application exposes the following rest URLs:

- GET on `http://$NARAYANA_HOST/api/`: list all messages in the `audit_log` table (ordered)
- POST on `http://$NARAYANA_HOST/api/?entry=xxx`: put a message `xxx` in the `incoming` queue for processing

#### Simple workflow

First get a list of messages in the `audit_log` table:

```
curl -w "\n" http://$NARAYANA_HOST/api/
```

The list should be empty at the beginning. Now you can put the first element.

```
curl -w "\n" -X POST http://$NARAYANA_HOST/api/?entry=hello
# wait a bit
curl -w "\n" http://$NARAYANA_HOST/api/
```

The new list should contain two messages: `hello` and `hello-ok`.

The `hello-ok` confirms that the message has been sent to a `outgoing` queue and then logged.
 
You can add multiple messages and see the logs. The following actions force the application in some corner cases 
to examine the behavior.

#### Exception handling

Send a message named `fail`:

```
curl -w "\n" -X POST http://$NARAYANA_HOST/api/?entry=fail
# wait a bit
curl -w "\n" http://$NARAYANA_HOST/api/
```

This message produces an exception at the end of the route, so that the transaction is always rolled back.

You should **not** find any trace of the message in the `audit_log` table.
If you check the application log, you'll find out that the message has been sent to the dead letter queue.

#### Unsafe system crash

Send a message named `killBeforeCommit`:

```
curl -w "\n" -X POST http://$NARAYANA_HOST/api/?entry=killBeforeCommit
# wait a bit (the pod should be restarted)
curl -w "\n" http://$NARAYANA_HOST/api/
```

This message produces a **immediate crash after the first phase of the 2pc protocol and before the final commit**.
The message **must not** be processed again, but the transaction manager was not able to send a confirmation to all resources.
If you check the `audit_log` table in the database while the application is down, you'll not find any trace of the message (it will appear later).

After **the pod is restarted** by Openshift, the **recovery manager will recover all pending transactions by communicating with the participating resources** (database and JMS broker).

When the recovery manager has finished processing failed transactions, you should find **two log records** in the `audit_log` table: `killBeforeCommit`, `killBeforeCommit-ok`.
