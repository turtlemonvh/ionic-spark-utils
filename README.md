# Ionic Spark Util

A set of utilities for working with Ionic encryption in Spark.

Main components include

* authentication (creating the initial device / Ionic agent)
* metadata (harvesting metadata for more interesting contexts)
* transformers (for working with dataframes)
* caching and key re-use
* testing patterns for more complex applications

## Status

Work in progress. Just getting going.

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
# Reload after changes to build.sbt and friends
> reload
```

Note that junit tests are often skipped by sbt. Running `clean` seems to consistently fix this behavior.

## Credits

* Project bootstrapped via: https://github.com/holdenk/sparkProjectTemplate.g8


