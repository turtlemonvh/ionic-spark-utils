# Ionic Spark Util

[![CircleCI](https://circleci.com/gh/turtlemonvh/ionic-spark-utils.svg?style=svg)](https://circleci.com/gh/turtlemonvh/ionic-spark-utils)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.turtlemonvh/ionicsparkutils_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.turtlemonvh/ionicsparkutils_2.12)
[![javadoc](https://javadoc.io/badge2/io.github.turtlemonvh/ionicsparkutils_2.12/javadoc.svg)](https://javadoc.io/doc/io.github.turtlemonvh/ionicsparkutils_2.12)

A set of utilities for working with [Ionic encryption](https://ionic.com/developers/) in [Spark](https://spark.apache.org/).

Main components include

* transformers (for working with dataframes)
* caching and key re-use
* mocks and other testing tools

Currently scala only, though [python and java support are planned](https://github.com/turtlemonvh/ionic-spark-utils/issues/9).

## Use

> See more in ["/examples"](https://github.com/turtlemonvh/ionic-spark-utils/tree/master/examples).

### Using the transformer

The core feature of this library is a Spark transformer that makes it easy to encrypt or decrypt columns.

```scala
import io.github.turtlemonvh.ionicsparkutils.KeyServicesCache;
import io.github.turtlemonvh.ionicsparkutils.{Transformers => IonicTransformers};
import com.ionic.sdk.agent.Agent
import com.ionic.sdk.device.profile.persistor.DeviceProfiles

def agentFactory(): KeyServices = {
  // Load profile JSON from whatever secure storage you have available
  // Each cloud provider has secret store interfaces that work well here
  val threadLocalAgent = new Agent(new DeviceProfiles(profileJson))
  // Wrap in a cache layer so that each a single key is used for each transform operation
  new KeyServicesCache(threadLocalAgent)
}

// A new column will be added named "ionic_enc_mycolumn"
// You probably want to call `.drop` and `.withColumnRenamed` on the
// resulting dataset to clean things up.
val encryptedDF = mydataset
.transform(IonicTransformers.Encrypt(
  encryptCols = List("mycolumn"),
  decryptCols = List(),
  agentFactory = agentFactory
))
```

## Status

Works for basic operations. Spark API is likely to change in future releases.

## Workflow

```bash
# Start a shell
$ sbt
# Compile the code
> compile
# Run the tests
> test
# Get a list of all tests
> show test:definedTests
# Run a subset of tests
> testOnly io.github.turtlemonvh.ionicsparkutils.TestAgentTest
# Reload after changes to build.sbt and friends
> reload
```

Junit tests are sometimes skipped by sbt. Running `clean` seems to consistently fix this behavior.  Test results are dumped in `target/test-reports/*.xml`.

## Credits

* Project bootstrapped via: https://github.com/holdenk/sparkProjectTemplate.g8
