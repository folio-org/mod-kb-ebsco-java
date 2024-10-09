## v5.0.0
### Breaking changes
* GET `/eholdings/kb-credentials/{credentialsId}/key` requires `kb-ebsco.kb-credentials.key.item.get` permission
* GET `/eholdings/kb-credentials/{id}/uc/key` requires `kb-ebsco.kb-credentials.uc.key.item.get` permission

### APIs versions
* Provides `eholdings v4.0`

### Tech Dept
* Cleanup permissions for eholdings interface ([MODKBEKBJ-769](https://issues.folio.org/browse/MODKBEKBJ-769))

### Dependencies
`* Remove `commons-configuration2`

## v4.1.0 2024-03-20
### Dependencies
* Bump `domain-models-runtime` from `35.0.6` to `35.2.0`
* Bump `folio-di-support` from `2.0.1` to `2.1.0`
* Bump `folio-service-tools` from `3.1.0` to `4.0.0`
* Bump `folio-holdingsiq-client` from `3.0.0` to `3.1.0`
* Bump `folio-liquibase-util` from `1.7.0` to `1.8.0`
* Bump `mod-configuration-client` from `5.9.2` to `5.10.0`
* Bump `vertx` from `4.4.6` to `4.5.5`
* Bump `aspectj` from `1.9.20.1` to `1.9.21.2`
* Bump `jackson` from `2.15.2` to `2.17.0`
* Bump `postgresql` from `42.6.0` to `42.7.3`
* Bump `commons-codec` from `1.16.0` to `1.16.1`
* Bump `commons-configuration2` from `2.9.0` to `2.10.0`
* Bump `lombok` from `1.18.30` to `1.18.32`
* Bump `jetbrains-annotations` from `24.0.1` to `24.1.0`
* Bump `opencsv` from `5.8` to `5.9`
* Bump `commons-lang3` from `3.13.0` to `3.14.0`
* Remove `javax.validation.validation-api`

## v4.0.0 2023-10-11
### Breaking changes
* Update the module for Java 17 & the latest dependencies ([MODKBEKBJ-732](https://issues.folio.org/browse/MODKBEKBJ-732))

### APIs versions
* Provides `eholdings v3.3`

### Features
* GET /eholdings/titles | Add Packages Facet to response and packageIds filter to request ([MODKBEKBJ-717](https://issues.folio.org/browse/MODKBEKBJ-717))
* GET /eholdings/resource | Changed Proxy to ProxiedUrl class in CustomResourceList from Title class ([MODKBEKBJ-740](https://github.com/folio-org/mod-kb-ebsco-java/pull/458))

### Tech Dept
* Logging improvement ([MODKBEKBJ-626](https://issues.folio.org/browse/MODKBEKBJ-626))
* Increase memory allocation in default LaunchDescriptor ([MODKBEKBJ-326](https://issues.folio.org/browse/MODKBEKBJ-326))
* Add logs for Usage Consolidation flow ([MODKBEKBJ-726](https://issues.folio.org/browse/MODKBEKBJ-726))
* Reduce the size of the page by half after failed retry for holdings loading ([MODKBEKBJ-747](https://issues.folio.org/browse/MODKBEKBJ-747))

### Bug fixes
* Load holdings process failing after exceeding status request retries ([MODKBEKBJ-713](https://issues.folio.org/browse/MODKBEKBJ-713))

### Dependencies
* Bump `java` from `11` to `17`
* Bump `folio-holdingsiq-client` from `2.3.0` to `3.0.0`
* Bump `folio-service-tools` from `1.10.1` to `3.1.0`
* Bump `folio-di-support` from `1.7.0` to `2.0.0`
* Bump `mod-configuration-client` from `5.9.1` to `5.9.2`
* Bump `aspectj` from `1.9.9.1` to `1.9.20.1`
* Bump `postgresql` from `42.5.3` to `42.6.0`
* Bump `jetbrains-annotations` from `24.0.0` to `24.0.1`
* Bump `commons-codec` from `1.15` to `1.16.0`
* Bump `commons-configuration2` from `2.8.0` to `2.9.0`
* Bump `commons-lang3` from `3.12.0` to `3.13.0`
* Bump `lombok` from `1.18.26` to `1.18.30`
* Bump `opencsv` from `5.7.1` to `5.8`
* Bump `log4j` from `2.19.0` to `2.20.0`
* Remove `aspectj-maven-plugin` with groupId `com.nickwongdev` & version `1.12.6`
* Add `aspectj-maven-plugin` with groupId `dev.aspectj` & version `1.13.1`

## v3.13.0 2023-02-15
### Bug Fixes
* Missing module permission for access status types ([MODKBEKBJ-708](https://issues.folio.org/browse/MODKBEKBJ-708))
* Load holdings: process failing after exceeding status request retries ([MODKBEKBJ-713](https://issues.folio.org/browse/MODKBEKBJ-713))

### Tech Dept
* Align logging configuration with common Folio solution ([MODKBEKBJ-699](https://issues.folio.org/browse/MODKBEKBJ-699))

### Dependencies
* Bump `folio-service-tools` from `1.10.0` to `1.10.1`
* Bump `folio-holdingsiq-client` from `2.2.0` to `2.3.0`
* Bump `folio-liquibase-util` from `1.5.0` to `1.6.0`
* Bump `mod-configuration-client` from `5.9.0` to `5.9.1`
* Bump `raml-module-builder` from `35.0.0` to `35.0.6`
* Bump `vertx` from `4.3.3` to `4.3.8`
* Bump `spring` from `5.3.23` to `5.3.25`
* Bump `jackson` from `2.13.4` to `2.14.2`
* Bump `postgresql` from `42.5.0` to `42.5.3`
* Bump `lombok` from `1.18.24` to `1.18.26`
* Bump `httpcore` from `4.4.15` to `4.4.16`
* Bump `jetbrains-annotations` from `23.0.0` to `24.0.0`
* Bump `opencsv` from `5.7.0` to `5.7.1`
* Bump `rest-assured` from `5.2.0` to `5.3.0`
* Bump `wiremock` from `2.34.0` to `2.35.0`
* Bump `mockito` from `4.8.0` to `5.1.1`
* Added `assertj` `3.24.2`

## v3.12.2 2022-12-09
* MODKBEKBJ-708 - Missing permission to see and use "Access status type" feature in "eHoldings" app

## v3.12.1 2022-11-16
* MODKBEKBJ-702 Vert.x 4.3.4, RMB 35.0.3, Jackson 2.14.0

## v3.12.0 2022-10-28
* MODKBEKBJ-673 User with permission "Settings (eHoldings): Can assign/unassign a user from a KB" can't assign users to KB credentials
* MODKBEKBJ-675 Add checkstyle plugin
* MODKBEKBJ-680 Fix holdings_status migration
* MODKBEKBJ-681 Usage Consolidation | Full text requests by platform usage doesn't display when selected "All platforms"/"Non-publisher platforms only" option
* MODKBEKBJ-682 Supports users interface versions 15.0 16.0
* MODKBEKBJ-688 Fix IndexOutOfBoundException if resource customerResourcesList is empty
* MODKBEKBJ-692 RMB v35 upgrade

## v3.11.2 2022-10-28
* MODKBEKBJ-695 Load Holdings: Add support for old statuses date format
* MODKBEKBJ-696 Load Holdings: Fix NullPointerException while reading started date
* MODKBEKBJ-697 Load Holdings: Increase size of publication_title field

## v3.11.1 2022-07-28
* MODKBEKBJ-681 Usage Consolidation: Fix NPE when no counts exist

## v3.11.0 2022-07-08
* MODKBEKBJ-621 User-assignment: Rework GET request for '/kb-credentials/{kb-credentials-id}/users'
* MODKBEKBJ-622 User-assignment: Remove parameters from POST request of '/kb-credentials/{kb-credentials-id}/users'
* MODKBEKBJ-623 User-assignment: Remove user-related data from module
* MODKBEKBJ-624 Add new package content types
* MODKBEKBJ-628 Define 'kb-ebsco.user-kb-credential.get' permission
* MODKBEKBJ-637 Implement GET /eholdings/uc-credentials/clientId endpoint
* MODKBEKBJ-638 Implement GET /eholdings/uc-credentials/clientSecret endpoint
* MODKBEKBJ-639 Add the ability to include accessType for package resources
* MODKBEKBJ-659 Upgrade RMB to v34.0.0 and Vert.x to v4.3.1
* MODKBEKBJ-662 User-assignment: sort assigned users by last name
* MODKBEKBJ-665 User-assignment: fix not all assigned users retrieved at response
* MODKBEKBJ-667 User-assignment: check if user exist before assign
* MODKBEKBJ-668 Fix loading holdings failed with NullPointer

## v3.10.0 2022-03-04
* MODKBEKBJ-593 Apply searchtype=advanced to Packages search query
* MODKBEKBJ-598 Packages, clarify error message when unselect non-custom package
* MODKBEKBJ-599 Apply searchtype=advanced to Titles search query
* MODKBEKBJ-600 Providers, deny updating tags with empty name
* MODKBEKBJ-601 User-assigment, deny assign user with empty name
* MODKBEKBJ-604 Return managed embargo in /costperuse endpoint
* MODKBEKBJ-609 Unable to update title-package record when user removes custom embargo
* MODKBEKBJ-611 Include alternateTitle information with GET/eholdings/titles response
* MODKBEKBJ-612 Include alternateTitle information with GET /eholdings/packages/{packageId}/resources response
* MODKBEKBJ-614 Update RMB to v33.2.6
* MODKBEKBJ-617 Remake constants to static methods
* MODKBEKBJ-618 Fix Null Custom embargo period

## v3.9.0 2021-10-07
* MODKBEKBJ-596 Packages, error message when "isCustom" not provided
* MODKBEKBJ-597 Packages, deny creating package with empty name

## v3.8.0 2021-06-10
* MODKBEKBJ-570 Filter resources in title
* MODKBEKBJ-578 Fix duplicate titles on GET by tags with included resources 
* MODKBEKBJ-577 Decrease size of holdings loading pages
* MODKBEKBJ-576 Implement GET UC credentials endpoint
* MODKBEKBJ-579 Implement PUT UC credentials endpoint
* MODKBEKBJ-581 Upgrade to RMB v33 and Vert.X v4.1.0.CR1

## v3.7.0 2021-03-09
* MODKBEKBJ-570 Fix percent of usage calculation
* MODKBEKBJ-551 Upgrade to RMB v32.1 and Vert.X v4
* Usage consolidation feature
* MODKBEKBJ-566 Fix processing include parameter while search titles by access types
* MODKBEKBJ-563 Add personal data disclosure form
* MODKBEKBJ-556 Fix holdings loading after dummy credentials update
* MODKBEKBJ-549 Suppress unnecessary logs
* MODKBEKBJ-536 Load holdings by id doesn't load when there are more than one credentials
* MODKBEKBJ-535 Fix titles in package filtering by tags

## v3.6.5 2020-12-22
* MODKBEKBJ-528 Fix hanged holding loading
* Upgrade to folio-holdingsiq-client v1.10.3

## v3.6.4 2020-12-14
* MODKBEKBJ-522 Fix filtering packages by tags
* MODKBEKBJ-537 Fix migration scripts

## v3.6.3 2020-11-30
* MODKBEKBJ-532 Fix filtering resources in packages by access types

## v3.6.2 2020-11-10
* Update to RMB v31.1.5 and Vert.x 3.9.4

## v3.6.1 2020-11-05
* MODKBEKBJ-505 Log responses from HoldingsIQ/MODKBEKBJ
* MODKBEKBJ-515 GET Titles search | change searchtype from advanced to contains

## v3.6.0 2020-09-12
* fix console logging
* Upgrade RMB to v31.1.0
* MODKBEKBJ-472 Return a better message when the title is no longer in a package
* Upgrade RMB to v31.0.0
* MODKBEKBJ-463 Send correct error message from HoldingsIQ
* MODKBEKBJ-462 Support package information returned on GET /titles
* MODKBEKBJ-454 Change search by Tags filter format
* MODKBEKBJ-435 Update DB schema to have separate user table

## v3.5.4 2020-11-05
* MODKBEKBJ-516 Change custom labels' max label length
* MODKBEKBJ-517 Change custom labels' max value length

## v3.5.3 2020-09-22
* MODKBEKBJ-501 Load Holdings timer process does not update holdings

## v3.5.2 2020-07-06
* MODKBEKBJ-449 Prevent duplicate KB credentials
* MODKBEKBJ-450 Remove restrictions to support deleting KB credentials
* MODKBEKBJ-452 Schema migration issue from Fameflower - Goldenrod

## v3.5.1 2020-06-18
* MODKBEKBJ-444 - Update migration scripts
* MODKBEKBJ-447 - Add support of PATCH credentials endpoint
* Upgrade eholdings interface to v2.1
* Upgrade RMB to v30.1.0

## v3.5.0 2020-06-12
* MODKBEKBJ-444 - Final verification migration scripts before release Q2 2020
* MODKBEKBJ-443 - Update to RMB v30
* MODKBEKBJ-430 - Support POST /eholdings/loading/kb-credentials to load holdings for all KB Credentials
* MODKBEKBJ-436 - Securing APIs by default
* MODKBEKBJ-427 - Update GET /loadHoldings/status to support getting status for certain credentials
* MODKBEKBJ-432 - Modify configuration routine to support user related KB credentials
* MODKBEKBJ-428 - Set loading status "Not Started" on KB Credentials creation
* MODKBEKBJ-434 - Apply credentials to services that work with local data of packages, providers, resources, titles
* MODKBEKBJ-429 - Support POST /eholdings/loading/kb-credentials/{credentialsId} to load holdings for certain KB Credentials
* MODKBEKBJ 413 - Update mapping and filtering by access types for packages and resources
* MODKBEKBJ-433 - Update RMAPITemplate to use new configuration routine
* MODKBEKBJ-432 - Modify configuration routine to support user related KB credentials
* MODKBEKBJ-438 - Update default RM API url
* MODKBEKBJ-406 - Update DELETE /eholdings/access-types/{id} to support multiple KB credentials
* MODKBEKBJ-418 - Update PUT /eholdings/root-proxy to support multiple KB credentials
* MODKBEKBJ-405 - Update PUT /eholdings/access-types/{id} to support multiple KB credentials
* MODKBEKBJ-404 - Update GET /eholdings/access-types/{id} to support multiple KB credentials
* MODKBEKBJ-403 - Update POST /eholdings/access-types to support multiple KB credentials
* MODKBEKBJ-417 - Update GET /eholdings/root-proxy to support multiple KB credentials
* MODKBEKBJ-416 - Update GET /eholdings/proxy-types to support multiple KB credentials
* MODKBEKBJ-402 - Update GET /eholdings/access-types to support multiple KB credentials
* MODKBEKBJ-415 - Update PUT /eholdings/custom-labels to support multiple KB credentials
* MODKBEKBJ-424 - Support GET /eholdings/user-kb-credential to retrieve KB credentials entry for user
* MODKBEKBJ-414 - Update GET /eholdings/custom-labels to support multiple KB credentials
* MODKBEKBJ-412 - Support DELETE /eholdings/kb-credentials/{credId}/users/{userId} to remove association between user and KB
* MODKBEKBJ-411 - Support PUT /eholdings/kb-credentials/{credId}/users/{userId} to update details of user associated with KB
* MODKBEKBJ-410 - Support POST /eholdings/kb-credentials/{credId}/users to associate user with KB
* MODKBEKBJ-408 - Support GET /eholdings/kb-credentials/{credId}/users to retrieve users associated with KBs
* MODKBEKBJ-423 - Support DELETE /eholdings/kb-credentials/{credentialsId} to delete KB credentials entry
* MODKBEKBJ-422 - Support PUT  /eholdings/kb-credentials{credentialsId} to update existing KB credentials entry
* MODKBEKBJ-421 - Support GET /eholdings/kb-credentials/{credentialsId} to retrieve particular KB credential entry
* MODKBEKBJ-420 - Support POST /eholdings/kb-credentials to create new KB credentials entry

## v3.4.0 2020-04-06
* MODKBEKBJ-356 - Filter titles by access type
* MODKBEKBJ-376 - Filter titles by access type into package
* MODKBEKBJ-384 - Apply sort to packages into title
* MODKBEKBJ-386 - Implement endpoint to bulk load packages records
* MODKBEKBJ-392 - Fix migrating from v3.2.0 to v3.3.1
* MODKBEKBJ-426 - Fix embargo time units are not properly converted

## v3.3.1 2020-03-13
* Set Load Holdings Strategy to DefaultLoadServiceFacade
* MODKBEKBJ-385 - Implement endpoint to bulk load resources records
* MODKBEKBJ-375 - Provider: Search within Packages: Filter Packages by Access Status Type

## v3.3.0 2020-03-11
* MODKBEKBJ-349 - Set access type on create custom package
* MODKBEKBJ-350 - Set access type on edit package
* MODKBEKBJ-353 - Set access type on update not custom package
* MODKBEKBJ-354 - Set access type on update resource
* MODKBEKBJ-355 - Implement package filtering by access type
* MODKBEKBJ-374 - Make max access types value be configurable
* MODKBEKBJ-377 - Add contributors list in GET /eholdings/titles response
* MODKBEKBJ-378 - Add number of records to GET /eholdings/access-types endpoints
* MODKBEKBJ-380 - Fix delete access type implementation

## v3.2.0 2020-02-11
* MODKBEKBJ-334 - Custom labels: Expand POST title payload
* MODKBEKBJ-335 - Custom labels: Expand PUT title payload
* MODKBEKBJ-336 - Custom labels: Expand GET title payload regardless of the package(s)
* MODKBEKBJ-337 - Custom labels: Expand GET title payload for package-title records
* MODKBEKBJ-340 - Custom labels: retrieve custom labels list
* MODKBEKBJ-341 - Custom labels: update custom labels list
* MODKBEKBJ-342 - Custom labels: retrieve custom label by id
* MODKBEKBJ-343 - Custom labels: delete custom label
* MODKBEKBJ-344 - Use new HoldingsIQ endpoints to use delta reports
* MODKBEKBJ-345 - Custom Labels: update existing implementation
* MODKBEKBJ-346 - Fix bug where resource is not removed if request has userDefinedFields
* MODKBEKBJ-348 - Settings: CRUD Access Status Types

## v3.1.0 2019-12-02
* MODKBEKBJ-333 - Set fromModuleVersion attribute for all tables
* MODKBEKBJ-339 - Update RMB version to 29.0.1
* MODKBEKBJ-320 - Add audit table for holdings_status
* MODKBEKBJ-338 - Manage container memory
* MODKBEKBJ-329 - Fix Security Vulnerability
* MODKBEKBJ-325 - Fix Issue with accessing eholdings with highly limited permissions
* MODKBEKBJ-324 - Add timeout to loading process
* MODKBEKBJ-321 - Create holdings snapshot only if it wasn't recently created
* MODKBEKBJ-311 - Don't override loading status when TenantAPI is called second time
* MODKBEKBJ-315 - Fix Security Vulnerability

## v3.0.1 2019-09-24
* MODKBEKBJ-311 - Add isFullPackage to schema for PUT /packages/{id} endpoint,

## v3.0.0 2019-09-10
* MODKBEKBJ-312 - Update RMB to version 27.0.0
* MODKBEKBJ-309 - Change frequency of loading to 5 days
* MODKBEKBJ-306 - Correctly process "None" status on loading holdings
* MODKBEKBJ-287 - Remove update of resource tags from resource endpoints
* MODKBEKBJ-286 - Remove update of provider tags from provider endpoints
* MODKBEKBJ-283 - Remove update of package tags from package endpoints
* MODKBEKBJ-294 - "Internal Server Error" appears after removing any title from Custom package
* MODKBEKBJ-284 - Add retry mechanism for loading holdings

## v2.5.1 2019-07-26
* MODKBEKBJ-293 - Add default timestamp to updated_at column
 
## v2.5.0 2019-07-24
* MODKBEKBJ-285 - Implement GET /loadHoldings/status
* MODKBEKBJ-282 - Add separate endpoint for updating resource tags
* MODKBEKBJ-281 - Add separate endpoint for updating provider tags
* MODKBEKBJ-263 - Add method to return list of Tags available for an entity type
* MODKBEKBJ-280 - Add separate endpoint for updating package tags
* MODKBEKBJ-266 - Resources: Make unit tests payloads more realistic
* MODKBEKBJ-265 - Title: Make unit tests payloads more realistic
* MODKBEKBJ-264 - Package: Make unit tests payloads more realistic
* MODKBEKBJ-251 - Provider: Make unit tests payloads more realistic
* MODKBEKBJ-273 - Holdings: Update save holdings flow
* MODKBEKBJ-271 - Provider/Package Search: Internal server error

## v2.4.0 2019-06-10
* MODKBEKBJ-187	- Tags: Update mod-kb-ebsco-java to search providers by tag filter only
* MODKBEKBJ-214	- Tags: add provider table to database
* MODKBEKBJ-215	- Tags: Cache providers that are retrieved by id
* MODKBEKBJ-218	- Tags: add title table to database
* MODKBEKBJ-219	- Tags: Cache titles that are retrieved by id
* MODKBEKBJ-220	- Create holdings table
* MODKBEKBJ-221	- Tags: add resources table to database
* MODKBEKBJ-222	- Tags: Cache resources that are retrieved by id
* MODKBEKBJ-227	- Modify ModuleDescriptor 
* MODKBEKBJ-230	- Filter by tags only - Resources
* MODKBEKBJ-236	- Tags: Update providers (search within packages ) to filter by tags only
* MODKBEKBJ-237	- Tags: Update packages (search within titles ) to filter by tags only
* MODKBEKBJ-238	- Create periodic task to populate holdings table
* MODKBEKBJ-240	- Provider | List of Packages | Display assigned package tags
* MODKBEKBJ-241	- Package | List of Titles | Display assigned resource tags
* MODKBEKBJ-242	- Title | List of Packages | Display assigned resource tags
* MODKBEKBJ-250	- Fix security vulnerabilities reported in jackson-databind >= 2.0.0, < 2.9.9

## v2.3.0 2019-05-08
* MODKBEKBJ-186	- Tags: add endpoint to return all tags assigned to records of particular type(s)
* MODKBEKBJ-188 - Tags: search packages by tag filter only
* MODKBEKBJ-201 - Spike: Define technical approach for using RM API to filter results by tags
* MODKBEKBJ-205 - Switching to managed coverage dates does not work
* MODKBEKBJ-208 - Contributor Type: Filter out any contributor type that is not Author, Editor,Illustrator
* MODKBEKBJ-209 - Do not capitalize contributor type
* MODKBEKBJ-210 - Tags: add package table to database
* MODKBEKBJ-211 - Tags: Cache packages that are retrieved by id
* MODKBEKBJ-214 - Add provider table to database
* MODKBEKBJ-215 - Add cache for providers 
* MODKBEKBJ-219 - Add cache for titles
* MODKBEKBJ-218 - Add title table to database
* MODKBEKBJ-221 - Add resource table to database
* MODKBEKBJ-222 - Add cache for resources 

## v2.2.0 2019-03-22
 * MODKBEKBJ-162- Tags: assign/unassign tags to Package record
 * MODKBEKBJ-163 - Tags: assign/unassign tags to Title record
 * MODKBEKBJ-165 - Tags: assign/unassign tags to Resource record
 * MODKBEKBJ-168 - Error when removing managed titles from holdings
 * MODKBEKBJ-174- Remove title from holdings - Error converting value \"Streaming Video\"
 * MODKBEKBJ-176 - Bug on adding resource to holdings
 * MODKBEKBJ-177- Add a method to modify title
 * MODKBEKBJ-181 - Investigate Newman error messages reason during collection run
 * MODKBEKBJ-182 - Tags: assign/unassign tags on provider when no package is selected 
 * MODKBEKBJ-183 - Tags: assign/unassign tags on resources regardless of isSelected value
 * MODKBEKBJ-194 - Custom Coverages are not returned in descending order
 * MODKBEKBJ-196 - Tags: assign/unassign tags on title regardless of whether it is managed or custom
 * MODKBEKBJ-197 - Tags: assign/unassign tags on package regardless of isSelected value
 * MODKBEKBJ-198 - DB upgrade fails
 * MODKBEKBJ-200 - Invalid filter for packages does not return ValidationException

## v2.0.3 2019-02-08
 * MODKBEKBJ-168 - Error when removing managed titles from holdings
 * MODKBEKBJ-161 - Tags: assign/unassign tags to Provider record
 * MODKBEKBJ-158 - Tags: retrieve tags assigned to Provider record
 * MODKBEKBJ-159 - Tags: retrieve tags assigned to Package record
 * MODKBEKBJ-160 - Tags: retrieve tags assigned to Title record
 * MODKBEKBJ-164 - Tags: retrieve tags assigned to Resource record

## v2.0.2 2019-02-01
 * MODKBEKBJ-155 - Refactor of Postman API tests
 
## v2.0.1 2019-01-22
 * MODKBEKBJ-2 - Setting up the project
 * MODKBEKBJ-4 - Rewrite Configuration
 * MODKBEKBJ-6 - Rewrite Proxy related endpoints
 * MODKBEKBJ-7 - Rewrite Providers endpoints
 * MODKBEKBJ-8 - Rewrite Packages endpoints
 * MODKBEKBJ-9 - Rewrite Resources endpoints
 * MODKBEKBJ-10 - Rewrite Titles endpoints
 * Replace mod-kb-ebsco (ruby version) with mod-kb-ebsco-java in environments

## v0.1.0 2018-10-04
 * Added raml files
 * Initial module setup
 * Added Jenkinsfile
 * Added Dockerfile
 * Updated README file
 * Added boilterplate files
 * Repository creation
