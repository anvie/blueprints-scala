import java.text.SimpleDateFormat

organization := "com.ansvia.graph"

name := "blueprints-scala"

version := "0.0.8-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions := Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
        "Ansvia repo" at "http://scala.repo.ansvia.com/releases/"
    )

libraryDependencies ++= Seq(
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.2.0",
    "com.tinkerpop.gremlin" % "gremlin-groovy" % "2.2.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % "2.2.0",
    "org.specs2" %%  "specs2" % "1.9" % "test",
    "com.tinkerpop.blueprints" % "blueprints-neo4j-graph" % "2.0.0" % "test"
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

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

//credentials += Credentials(Path.userHome / ".sbt" / "0.11.2" / "sonatype.sbt")

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
