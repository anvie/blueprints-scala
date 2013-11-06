resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/snapshots"

// SCCT
addSbtPlugin("com.github.scct" % "sbt-scct" % "0.3-SNAPSHOT")

addSbtPlugin("com.github.theon" %% "xsbt-coveralls-plugin" % "0.0.4")
