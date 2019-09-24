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
