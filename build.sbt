name := "sflib"

organization := "ca.shiftfocus"

version := "1.0.8"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.10.4", "2.11.6", "2.12.4")

val root = project in file(".")

publishMavenStyle := true

publishTo := {
  val privateKeyFile = new java.io.File(sys.env("HOME") + ".ssh/id_rsa")
  Some(Resolver.sftp(
    "ShiftFocus Maven Repository",
    "maven.private.shiftfocus.ca",
    22,
    "/var/www/maven.shiftfocus.ca/repositories/" + {
      if (isSnapshot.value) "snapshots" else "releases"
    }
  ) as ("maven", privateKeyFile))
}

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "ShiftFocus repository" at "https://maven.shiftfocus.ca/repositories/releases/"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.20"
)
