organization := "com.ansvia.graph"

name := "blueprints-scala"

version := "0.0.6-SNAPSHOT"

scalaVersion := "2.10.0"

scalacOptions := Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
	"Ansvia repo" at "http://scala.repo.ansvia.com/releases/"
    )

libraryDependencies ++= Seq(
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.2.0",
    "com.tinkerpop.gremlin" % "gremlin-groovy" % "2.2.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % "2.2.0",
    "org.scala-lang" % "scala-reflect" % "2.10.0",
    "org.specs2" % "specs2_2.10" % "1.13" % "test",
    "com.tinkerpop.blueprints" % "blueprints-neo4j-graph" % "2.2.0" % "test"
    )

publishTo <<= version { (v:String) =>
      val ansviaRepo = "http://scala.repo.ansvia.com/nexus"
      if(v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at ansviaRepo + "/content/repositories/snapshots")
      else
          Some("releases" at ansviaRepo + "/content/repositories/releases")
  }

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

crossPaths := false

pomExtra := (
    <url>http://ansvia.com</url>
    <licenses>
      <license>
        <name>Apache 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
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
    </developers>)
