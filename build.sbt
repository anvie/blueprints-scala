import java.text.SimpleDateFormat
import sbt.Keys._
import sbt.Tests
import scala.Some

organization := "com.ansvia.graph"

name := "blueprints-scala"

version := "0.1.15-SNAPSHOT"

scalaVersion := "2.9.2"

scalacOptions := Seq("-unchecked", "-deprecation")

testOptions in Test ++= Seq(Tests.Argument("sequential"), Tests.Argument("stopOnFail"))

resolvers ++= Seq(
        "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
        "Ansvia repo" at "http://scala.repo.ansvia.com/releases/"
    )

libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.6",
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.3.0",
    "com.tinkerpop.gremlin" % "gremlin-groovy" % "2.3.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % "2.3.0",
    "org.specs2" %%  "specs2" % "1.12.3" % "test",
    "com.tinkerpop.blueprints" % "blueprints-neo4j-graph" % "2.3.0" % "test"
    )

publishTo <<= version { (v:String) =>
      //val repoUrl = "http://scala.repo.ansvia.com/nexus"
      val repoUrl = "https://oss.sonatype.org"
      if(v.trim.endsWith("SNAPSHOT") || """.+\-\d{8}+$""".r.pattern.matcher(v.trim).matches())
          Some("snapshots" at repoUrl + "/content/repositories/snapshots")
      else
          Some("releases" at repoUrl + "/service/local/staging/deploy/maven2")
          //Some("releases" at repoUrl + "/content/repositories/releases")
  }

version <<= version { (v:String) =>
    if (v.trim.endsWith("-SNAPSHOT")){
        val dateFormatter = new SimpleDateFormat("yyyyMMdd")
        v.trim.split("-").apply(0) + "-" + dateFormatter.format(new java.util.Date()) + "-SNAPSHOT"
    }else
        v
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials-sonatype")

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
        <id>vikraman</id>
        <name>Vikraman Choudhury</name>
        <url>http://vh4x0r.wordpress.com</url>
      </developer>
    </developers>)
