# spring-boot-camel-xa


Scripts:

```
oc create -f persistent-volume-claim.yml
oc env dc/postgresql POSTGRESQL_MAX_PREPARED_TRANSACTIONS=100
```
