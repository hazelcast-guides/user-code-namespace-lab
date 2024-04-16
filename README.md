# Overview 

This is a short demonstration of user code namespaces.  In this lab we will be working 
with a spring boot service that uses an entry-processor to perform a basic authorization 
and then we will update the entry-processor without restarting the cluster.

# Prerequisites 

You will need a development laptop with the following installed:
- Docker Desktop
- A functional Java IDE
- Maven
- A Hazelcast Enterpise License that enables user code namespaces

# Overview of the Environment

This lab run within an isolated Docker environment. 

__TODO__

# Walk Through

### Start Up

Copy `sample.env` to `.env`.

Edit `.env` to include your license key.

Then build the project and bring up the environment using Docker.

```shell
mvn clean install
docker compose up -d 
```

### Review `hazelcast.yaml`

```yaml
hazelcast:
  user-code-namespaces:
    enabled: true
    card-ns: []

  jet:
    enabled: true
    resource-upload-enabled: true

  map:
    cards:
      user-code-namespace: card-ns
```

__NOTES__

1. Note how the `card-ns` namespace is declared under the `user-code-namespaces` key
and then associated with the `cards` map below.  In addition to IMaps,
most of the other data structures can be associated with a user code namespace.
2. You can also associate a user code namespace with a declared executor 
service allowing you to associate arbitrary server side code with a user
code namespace,  _which is cool, and potentially very useful._
3. Underneath the declaration of the specific namespace you can see
that, in the example, there is an empty list.  You can statically 
specify resources (jars, classes) here but in this example we will 
specify them dynamically

### Review the Code and Configuration

- [ ] Review the `TransactionEntryProcessor` in the `common` project.
- [ ] Review the `AuthorizationServiceController` in the `authorization-service` project.

```java
    # this code runs once at startup 
    @PostConstruct
    public void init(){
        // use the default instantiation process
        hz = HazelcastClient.newHazelcastClient();
        cardMap = hz.getMap(Names.CARD_MAP_NAME);

        // send the TransactionEntryProcessor and Transaction classes to the cluster
        UserCodeNamespaceConfig ns = new UserCodeNamespaceConfig("card-ns");
        ns.addClass(TransactionEntryProcessor.class, Transaction.class);
        hz.getConfig().getNamespacesConfig().addNamespaceConfig(ns);
    }
```

- [ ] Check `compose.yaml`, note that nothing has been added to the 
hazelcast class path.

```yaml
  hz:
    image: hazelcast/hazelcast-enterprise:5.4.0-SNAPSHOT
    environment:
      JAVA_OPTS: -Dhazelcast.config=/project/hazelcast.yaml
      HZ_LICENSEKEY: ${HZ_LICENSEKEY}
    volumes:
      - ".:/project"
    networks:
      - hznet
```


__NOTES__

1. The EntryProcessor will be invoked on the "cards" map which contains 
instances of `Card`.  However, as you can see the type of the entry 
declared in the EntryProcessor is `GenericRecord`.  This is the recommended 
approach for map entries.  _It is not advisable to put the domain object
in the user code namespace_ because, when the contents of the namespace 
changes, a new class loader will be created and Java will consider 
a `Card` loaded from one class loader to be different from a 
`Card` loaded from another class loader, even if they are in the same
user code namespace.  This is the genesis of the "instance of class Card
cannot be cast to Card" error message.
2. In the `AuthorizationServiceController.init` method, notice how the 
`TransactionEntryProcessor` and `Transaction` classes are being added 
to the UserCodeNamespaceConfig every time at startup.
3. Also in `AuthorizationServiceController.init`, notice that the `Card`
class is __not__ specified.  If it is present in the user code namespace
for the map, it will be deserialized as `Card` and not `GenericRecord`, 
leading to the issues mentioned above.

### Test The Service 

Open up the management center at http://localhost:8080. Open the 
SQL tab and run "SELECT * FROM cards LIMIT 10".  Obtain a valid 
card number.  

Then, using something like Postman, POST to http://localhost:8888/authorize
The request should include an "application/json" Content-type header and 
the body should be similar to the one shown below.

```json
{
    "card_number":"3529-4419-5553-3395",
    "transaction_id": 999,
    "amount": 25,
    "merchant_id": 99,
    "status": "NEW"
}
```

### Update the Service

Edit the EntryProcessor.  You can, for example, just uncomment the 
code provided.  When you are done, rebuild and re-start the service.

```bash
mvn clean install
docker compose restart authorization-service
```
That is all you need to do to dynamically deploy an EntryProcessor!

You can now retest and see that the service now reflects 
your changes.
