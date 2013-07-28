name := "server-compare"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.1.0")

resolvers ++= Seq(
  "sonatype-public" at "https://oss.sonatype.org/content/groups/public")

scalaVersion := "2.10.2"