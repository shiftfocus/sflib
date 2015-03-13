# ShiftFocus Library

This repository is meant to hold various utility and helper classes that are not substantial nor special enough
to merit their own separate repository.

### Packages:

  * `ca.shiftfocus.lib.concurrent`: various helpers for working with Futures, scalaz.\/, and scalaz.EitherT
  * `ca.shiftfocus.lib.exceptions`: helpers for working with Exceptions, such as the ExceptionWriter
  * `ca.shiftfocus.lib.uuid`: additional helpers for working with UUIDs, such as the UuidPathBinder

### Usage

Include the Shiftfocus repository in your `build.sbt` file:

    resolvers += "ShiftFocus" at "https://maven.shiftfocus.ca/repositories/releases"

And then you can add the sflib library to your dependencies.

    "ca.shiftfocus" %% "sflib" % "1.0.1"

The latest release is `1.0.1`. The latest snapshot release is `1.0-SNAPSHOT`. Be sure to modify the resolver URL
to snapshots if you wish to use the snapshot release.