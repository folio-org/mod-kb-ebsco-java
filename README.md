# mod-kb-ebsco-java

[![FOLIO](https://img.shields.io/badge/FOLIO-Module-green)](https://www.folio.org/)
[![Release Version](https://img.shields.io/github/v/release/folio-org/mod-kb-ebsco-java?sort=semver&label=Latest%20Release)](https://github.com/folio-org/mod-kb-ebsco-java/releases)
[![Java Version](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.folio%3Amod-kb-ebsco-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=org.folio%3Amod-kb-ebsco-java)

Copyright © 2018–2025 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

<!-- TOC -->
* [mod-kb-ebsco-java](#mod-kb-ebsco-java)
  * [Introduction](#introduction)
  * [Overview](#overview)
  * [Getting Started](#getting-started)
    * [Prerequisites](#prerequisites)
    * [Building the Module](#building-the-module)
    * [Docker](#docker)
    * [Running the Module](#running-the-module)
  * [Additional Information](#additional-information)
    * [Issue tracker](#issue-tracker)
    * [Contributing](#contributing)
<!-- TOC -->

## Introduction

A FOLIO module for managing EBSCO knowledge base resources. This module provides broker communication with 
the EBSCO knowledge base, enabling management of electronic resources, providers, packages, and titles.

## Overview

The `mod-kb-ebsco-java` module serves as the integration layer between FOLIO and 
the EBSCO HoldingsIQ knowledge base service, providing APIs for managing electronic resources and holdings data.

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Docker (optional, for containerized deployment)

### Building the Module

Compile and build the module using Maven:

```shell
mvn clean install
```

See that it says "BUILD SUCCESS" near the end.

### Docker

Build the Docker container:

```shell
docker build -t mod-kb-ebsco-java .
```

Test that it runs:

```shell
docker run -t -i -p 8081:8081 mod-kb-ebsco-java
```

### Running the Module

The module can be deployed as part of a FOLIO instance. For detailed deployment instructions, refer to the [FOLIO deployment documentation](https://dev.folio.org/guides/automation/).

## Additional Information

**Related modules:**
- [folio-holdingsiq-client](https://github.com/folio-org/folio-holdingsiq-client) - HoldingsIQ API client library
- [ui-eholdings](https://github.com/folio-org/ui-eholdings) - User interface for managing e-holdings

For more FOLIO developer documentation, visit [dev.folio.org](https://dev.folio.org/)

### Issue tracker

See project [MODKBEKBJ](https://issues.folio.org/browse/MODKBEKBJ)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

### Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
