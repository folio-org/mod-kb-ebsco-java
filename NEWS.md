## v3.6.0 2020-09-12
* fix console logging
* Upgrade RMB to v31.1.0
* MODKBEKBJ-472 Return a better message when the title is no longer in a package
* Upgrade RMB to v31.0.0
* MODKBEKBJ-463 Send correct error message from HoldingsIQ
* MODKBEKBJ-462 Support package information returned on GET /titles
* MODKBEKBJ-454 Change search by Tags filter format
* MODKBEKBJ-435 Update DB schema to have separate user table

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
* MODKBEKBJ-432 - Modify configuration routine to support user related KB credentials
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
* MODKBEKBJ-386 - Implement endpoint to bulk load packages records
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
* MODKBEKBJ-348 - Settings: CRUD Access Status Types

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
