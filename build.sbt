import java.text.SimpleDateFormat

import SonatypeKeys._

organization := "com.ansvia.graph"

name := "blueprints-scala"

version := "0.1.61-SNAPSHOT"

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.11.0", "2.10.0")

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions")

resolvers ++= Seq(
    "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Ansvia repo" at "http://scala.repo.ansvia.com/releases/"
)

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.6",
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.5.0",
    "com.tinkerpop.gremlin" % "gremlin-groovy" % "2.5.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % "2.5.0",
    "org.specs2" %% "specs2-core" % "2.4.15" % "test",
    "com.thinkaurelius.titan" % "titan-core" % "0.5.2" % "provided",
    "com.thinkaurelius.titan" % "titan-berkeleyje" % "0.5.2" % "test"
)


scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
        // if scala 2.11+ is used, be strict about compiler warnings
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            Seq("-Xfatal-warnings")
        case _ =>
            Nil
    }
}



sonatypeSettings

profileName := "com.ansvia"

publishTo <<= version { (v:String) =>
    val nexus = "https://oss.sonatype.org"
    if(v.trim.endsWith("SNAPSHOT") || """.+\-\d{8}+$""".r.pattern.matcher(v.trim).matches())
        Some("snapshots" at nexus + "/content/repositories/snapshots")
    else
        Some("releases"  at nexus + "/service/local/staging/deploy/maven2")
}

version <<= version { (v:String) =>
    if (v.trim.endsWith("-SNAPSHOT")){
        val dateFormatter = new SimpleDateFormat("yyyyMMdd")
        v.trim.split("-").apply(0) + "-" + dateFormatter.format(new java.util.Date()) + "-SNAPSHOT"
    }else
        v
}

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
            <developer>
                <id>pdolega</id>
                <name>Pawel Dolega</name>
                <url>http://nexelem.com</url>
            </developer>
        </developers>)
