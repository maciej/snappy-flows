# Releasing a new version

Maintainer manual on releasing a new version.

## Preconditions

1. Install [sbt](https://www.scala-sbt.org) if necessary.
2. Ensure bintray credentials are present.
3. Ensure a valid PGP secret key for signing artifacts is present on the machine.

Run:

```shell
sbt release
```

