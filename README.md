# mod-kb-ebsco-java

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

* [Introduction](#introduction)
* [Compiling](#compiling)
* [Docker](#docker)
* [Installing the module](#installing-the-module)
* [Deploying the module](#deploying-the-module)

## Introduction
Broker communication with the EBSCO knowledge base (Java)

## API

Module provides next API:

 | METHOD   |  URL                                         | DESCRIPTION                                                                           |
 |----------|----------------------------------------------|---------------------------------------------------------------------------------------|
 | GET      | /eholdings/packages                          | Retrieve a collection of packages.                                                    |
 | POST     | /eholdings/packages                          | Create a new package.                                                                 |
 | GET      | /eholdings/packages/{packageId}              | Retrieve a specific package by packageId.                                             |
 | PUT      | /eholdings/packages/{packageId}              | Update a specific package using packageId.                                            |
 | DELETE   | /eholdings/packages/{packageId}              | Delete a specific package by packageId.                                               |
 | GET      | /eholdings/packages/{packageId}/resources    | Include all resources belonging to a specific package.                                |
 | GET      | /eholdings/providers                         | Get a list of providers.                                                              |
 | GET      | /eholdings/providers/{provider_id}           | Get the provider by provider_id.                                                      |
 | PUT      | /eholdings/providers/{provider_id}           | Update provider by provider_id.                                                       |
 | GET      | /eholdings/providers/{provider_id}/packages  | Retrieve a list of packages for specific provider.                                    |
 | POST     | /eholdings/resources                         | Create a relation between an existing package and an existing custom/managed title.   |
 | GET      | /eholdings/resources/{resourceId}            | Retrieve a specific resource by resourceId.                                           |
 | PUT      | /eholdings/resources/{resourceId}            | Update a specific resource using resourceId                                           |
 | DELETE   | /eholdings/resources/{resourceId}            | Delete the association between a custom/managed title and a package using resourceId. |
 | GET      | /eholdings/titles                            | Get a set of titles matching the given search criteria.                               |
 | GET      | /eholdings/titles/{title_id}                 | Get the title by title_id.                                                            |
 | POST     | /eholdings/titles/{title_id}                 | Create a new Custom Title.                                                            |
 | GET      | /eholdings/proxy-types                       | Get a list of supported root proxy types.                                             |
 | GET      | /eholdings/root-proxy                        | Get a list of Root Proxy.                                                             |
 | PUT      | /eholdings/root-proxy                        | Update the current root proxy.                                                        |
 | GET      | /eholdings/configuration                     | Get KB configuration currently being used.                                            |
 | PUT      | /eholdings/configuration                     | Update the currently set KB configuration.                                            |
 | GET      | /eholdings/status                            | Retrives status of current KB configuration.                                          |

## Compiling

```
   mvn install
```

See that it says "BUILD SUCCESS" near the end.

## Docker

Build the docker container with:

```
   docker build -t mod-kb-ebsco-java .
```

Test that it runs with:

```
   docker run -t -i -p 8081:8081 mod-kb-ebsco-java
```

## Installing the module

Follow the guide of
[Deploying Modules](https://github.com/folio-org/okapi/blob/master/doc/guide.md#example-1-deploying-and-using-a-simple-module)
sections of the Okapi Guide and Reference, which describe the process in detail.

First of all you need a running Okapi instance.
(Note that [specifying](../README.md#setting-things-up) an explicit 'okapiurl' might be needed.)

```
   cd .../okapi
   java -jar okapi-core/target/okapi-core-fat.jar dev
```

We need to declare the module to Okapi:

```
curl -w '\n' -X POST -D -   \
   -H "Content-type: application/json"   \
   -d @target/ModuleDescriptor.json \
   http://localhost:9130/_/proxy/modules
```

That ModuleDescriptor tells Okapi what the module is called, what services it
provides, and how to deploy it.

## Deploying the module

Next we need to deploy the module. There is a deployment descriptor in
`target/DeploymentDescriptor.json`. It tells Okapi to start the module on 'localhost'.

Deploy it via Okapi discovery:

```
curl -w '\n' -D - -s \
  -X POST \
  -H "Content-type: application/json" \
  -d @target/DeploymentDescriptor.json  \
  http://localhost:9130/_/discovery/modules
```

Then we need to enable the module for the tenant:

```
curl -w '\n' -X POST -D -   \
    -H "Content-type: application/json"   \
    -d @target/TenantModuleDescriptor.json \
    http://localhost:9130/_/proxy/tenants/<tenant_name>/modules
```




### Issue tracker

See project [MODKBEKBJ](https://issues.folio.org/browse/MODKBEKBJ)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).
