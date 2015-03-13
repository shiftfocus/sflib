name := "sflib"

organization := "ca.shiftfocus"

version := "1.0.0"

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.4", "2.11.6")

val root = project in file(".")

publishMavenStyle := true

publishTo := {
  val privateKeyFile = new java.io.File(sys.env("HOME") + "/.ssh/id_rsa")
  Some(Resolver.sftp(
    "ShiftFocus Maven Repository",
    "maven.shiftfocus.ca",
    50022,
    "/var/www/maven.shiftfocus.ca/repositories/" + {
      if (isSnapshot.value) "snapshots" else "releases"
    }
  ) as ("maven", privateKeyFile))
}

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.3.6",
  "org.scalaz" %% "scalaz-core" % "7.1.1",
  "ca.shiftfocus" %% "uuid" % "1.0.0"
)
