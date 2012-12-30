Tinkerpop Blueprints Scala
===================================

Scala wrapper for tinkerpop blueprints, this library provide more scalastic code when working with graph database
supported by blueprints.

Example
--------

More working and complete examples can be found on specs test.

Creating edges:

	hercules --> "father" --> jupiter
	hercules --> "mother" --> alcmene

Creating mutual (both) edges:

	jupiter <--> "brother" <--> neptune

Or more complex mutual edges:

	jupiter <--> "brother" <--> neptune <--> "brother" <--> pluto

Install
---------

Add resolvers:

	"Ansvia repo" at "http://scala.repo.ansvia.com/releases/"

Add dependencies:

	"com.ansvia.graph" % "blueprints-scala" % "0.0.1"


***[] Robin Sy.***
