import java.text.SimpleDateFormat

import SonatypeKeys._

organization := "com.ansvia.graph"

name := "blueprints-scala"

version := "0.1.41-SNAPSHOT"

scalaVersion := "2.10.0"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions")

resolvers ++= Seq(
    "nexelem repo" at "http://nexus.nexelem.com:8081/nexus/content/groups/public"
    )

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % "2.10.0",
    "org.slf4j" % "slf4j-api" % "1.7.6",
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.5.0",
    "com.tinkerpop.gremlin" % "gremlin-groovy" % "2.5.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % "2.5.0",
    "org.specs2" %% "specs2" % "1.14" % "test",
    "com.thinkaurelius.titan" % "titan-core" % "0.5.0" % "provided",
    "com.thinkaurelius.titan" % "titan-berkeleyje" % "0.5.0" % "test"
    )

sonatypeSettings

profileName := "com.ansvia"

publishTo := Some("Nexelem Nexus" at "http://nexus.nexelem.com:8081/nexus/content/repositories/snapshots")

credentials += Credentials {
    val sonatype = Path.userHome / ".ivy2" / ".credentials-sonatype"
    if (new File(sonatype.toString).exists())
        sonatype
    else
        Path.userHome / ".ivy2" / ".credentials"
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

crossPaths := true

publishMavenStyle := true

pomExtra := (
    <url>http://www.ansvia.com</url>
    <licenses>
      <license>
        <name>Apache 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:anvie/blueprints-scala.git</url>
      <connection>scm:git:git@github.com:anvie/blueprints-scala.git</connection>
    </scm>
    <developers>
      <developer>
        <id>robin</id>
        <name>Robin Syihab</name>
        <url>http://mindtalk.com/u/robin</url>
      </developer>
      <developer>
        <id>tysonjh</id>
        <name>Tyson Hamilton</name>
        <url>http://tysonhamilton.com</url>
      </developer>
      <developer>
        <id>vikraman</id>
        <name>Vikraman Choudhury</name>
        <url>http://vh4x0r.wordpress.com</url>
      </developer>
    </developers>)
