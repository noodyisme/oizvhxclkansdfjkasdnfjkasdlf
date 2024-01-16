# Policy Core Lite
[![Build Status](https://identitypl-n1jenkins.cloud.capitalone.com/buildStatus/icon?job=Bogie/identitybuilder/identity-builder-policy-core-lite/main)](https://identitypl-n1jenkins.cloud.capitalone.com/job/Bogie/job/identitybuilder/job/identity-builder-policy-core-lite/job/main/)
A lightweight framework for loading and executing policies.
---
## Getting Started

These instructions will get your application ready to interface with Policy Core Lite.

### Installing

##### Maven
```xml
<dependency>
    <groupId>com.capitalone.identity.platform</groupId>
    <artifactId>identity-platform-policy-core-lite</artifactId>
    <version>{PolicyCoreLiteVersion}</version>
</dependency>
```

##### Gradle
```groovy
dependencies {
    implementation "com.capitalone.identity.platform:identity-platform-policy-core-lite:${PolicyCoreLiteVersion}"
}
```
##### Artifactory
https://artifactory.cloud.capitalone.com/ui/packages/gav:%2F%2Fcom.capitalone.identity.platform:identity-platform-policy-core-lite?name=identity-platform-policy-core-lite&type=packages
---
## Testing

### Reporting
HTML Test reports will be automatically generated in the `build/reports/tests` directory for each test suite run. The index.html file is human-readable when opened in a web browser, and a great place to verify test runs manually. Results are also available in .xml format in the `build/test-results` directory for each test suite run. These .xml files are useful for machine verification, in places like a pipeline deployment.

### Functional Testing

To run all functional tests and generate a report, run the following command via the command-line:
```bash
./gradlew clean allTests
```

To only run unit tests, run the following command via the command-line:
```bash
./gradlew clean test
```

To run specific test(s) via the command-line, please reference the [Gradle Docs](https://docs.gradle.org/current/userguide/java_testing.html#:~:text=fully%2Dqualified%20names.-,Simple%20name%20pattern,-Since%204.7%2C%20Gradle) on running a test based on name pattern. 

Here is an example using a fully-qualified name + wildcard class pattern: 
```bash
./gradlew test --tests 'com.capitalone.identity.identitybuilder.platform.packageName*'
```

### Integration Testing
The Integration Test suite is hosted via the [integrationTest](src/integrationTest) directory. This is done to ensure that our integration tests are run on sources that are isolated from production code. 

To only run all integration tests, use the following command via command-line:
```bash
./gradlew clean integrationTest
```
To add a test to the Integration Test suite, just add the test class to the appropriate subdirectory within the `integrationTest` parent directory. Additional source and resource directories, as well as test suite specific dependencies, may be added via the testing.suites.integrationTest block in the build.gradle file. 

### Performance Testing
#### Micro-benchmarking
Our micro-benchmark tests are built on top of the JMH gradle plugin.
To run the benchmark tests, run the following command from the project root directory:
```bash
./gradlew clean jmh
```
The benchmark tests will run and the results will be saved in the `benchmark-results.csv` file, found in the `build/test-results/benchmarking` directory.

At least one benchmark test should be built and maintained for every feature, but more can be added as desired. The test classes and resources should be added to the [src/jmh/java](src/jmh/java) and [src/jmh/resources](src/jmh/resources) directories respectively. 

Note: Resources that already exist in the `main/resources` or `test/resources` modules, may be referenced in this module as well.

For more information on configuring these tests or the benchmark test suite itself, please see the jmh-gradle-plugin repo's [README](https://github.com/melix/jmh-gradle-plugin#readme).

## Local Development
### Publishing a local maven artifact
Use the following command to publish a maven artifact (snapshot) locally
```
gradle clean build publishToMavenLocal --info
```
Navigate to the local snapshot to confirm:
```bash
cd ~/.m2/repository/com/capitalone/identity/platform/identity-builder-policy-core-lite
ls
```
There should be a directory named based on the version. If no version declared this will be "unspecified". Navigate to the directory
and confirm that the file was updated. Of if desired declare a 'version' in the build.gradle (this should just be for local development)
```bash 
cd unspecified
ls -l
```
The time when the identity-builder-core-event-models-unspecified.jar was last updated from the 'ls -l' should relect a recent update.
Import the snapshot as a dependency as follows if version is unspecified (otherwise import based on the [version]-SNAPSHOT)
```
<dependency>
  <groupId>com.capitalone.identity.platform</groupId>
  <artifactId>identity-builder-policy-core-lite</artifactId>
  <version>unspecified</version>
</dependency>
```
# oizvhxclkansdfjkasdnfjkasdlf
