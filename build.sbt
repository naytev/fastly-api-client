name := "fastly-api-client"

organization := "com.gu"

version := "0.3.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "org.scalatest" %% "scalatest" % "2.1.2" % "test",
  "com.typesafe" % "config" % "1.2.0" % "test"
)

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding", "UTF8",
  "–explaintypes",
  "-feature",
  "–optimise",
  "-unchecked",
  "–Xcheck-null",
  "–Xcheckinit",
  "–Xlog-implicits",
  "–Xlint",
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-language:postfixOps"
)

// http://www.scala-sbt.org/release/docs/Howto/scaladoc.html
scalacOptions in (Compile,doc) := Seq("-groups", "-implicits")

publishMavenStyle := true

publishTo in ThisBuild <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at s"${nexus}content/repositories/snapshots")
    else
      Some("releases" at s"${nexus}service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := {
  _ => false
}

pomExtra := (
  <url>https://github.com/guardian/fastly-api-client</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:guardian/fastly-api-client.git</url>
      <connection>scm:git:git@github.com:guardian/fastly-api-client.git</connection>
    </scm>
    <developers>
      <developer>
        <id>obrienm</id>
        <name>Matthew O'Brien</name>
        <url>http://www.theguardian.com</url>
      </developer>
    </developers>
  )
