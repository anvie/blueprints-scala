Tinkerpop Blueprints Scala [![Build Status](https://secure.travis-ci.org/twitter/scalding.png)](http://travis-ci.org/anvie/blueprints-scala)
===================================

Scala wrapper for tinkerpop blueprints, this library provide more scalastic code when working with graph database
supported by blueprints.

Usage
---------

For installation see install section.
More working and complete examples can be found on specs test.
This example data based on graph of the gods https://github.com/thinkaurelius/titan/wiki/Getting-Started

![](https://github.com/thinkaurelius/titan/raw/master/doc/images/graph-of-the-gods.png)

Import all needed implicit definition:

	import com.ansvia.graph.BlueprintsWrapper._

Done, now you can using Scalastic sweet syntactic sugar code.

Creating edges:

	hercules --> "father" --> jupiter
	hercules --> "mother" --> alcmene

Or you can also add multiple edges in one line:

	hercules --> "father" --> jupiter --> "father" --> "saturn"

Creating mutual (both) edges:

	jupiter <--> "brother" <--> neptune

Or more complex mutual edges:

	jupiter <--> "brother" <--> neptune <--> "brother" <--> pluto
	

Shorthand property getter and setter:

	jupiter.set("kind", "god")
	
	val kind = jupiter.get[String]("kind").get
	
Getter `get` return Option instance, so you can handle unexpected return data efficiently using map:

	jupiter.get[String]("kind") map { kind =>
		// do with kind here
	}

Or getting value by using default value if empty:

	jupiter.getOrElse[String]("status", "live")

Easy getting mutual connection, for example jupiter and pluto is brother both has IN and OUT edges,
is very easy to get mutual connection using `mutual` method:

	val jupitersBrothers = jupiter.mutual('brother')
	
Inspect helpers to print vertex list:

	jupitersBrothers.printDump("jupiter brothers:", "name")
	
	// will produce output:

	jupiter brothers:
	 + neptune
	 + pluto

Using Gremlin Pipeline like a boss:

	val battled = hercules.pipe.out("battled")
	battled.printDump("hercules battled:", "name")
	
	// output:
	
	hercules battled:
	 + nemean
	 + hydra
	 + cerberus

Syntactic sugar filtering on Gremlin Pipeline:

	// get monster battled with hercules more than 5 times
	val monsters = hercules.pipe.outE("battled").wrap.filter { edge =>
	   edge.getOrElse[Int]("time", 0).toString.toInt > 5
	}

Returning edges from chain by adding `<` on the last line:

	var edge = hercules --> "battled" --> hydra <
	edge.set("time", 2)

Using transaction:

	transact {
		hercules --> "father" --> jupiter
	}


Install
--------

Add resolvers:

	"Ansvia repo" at "http://scala.repo.ansvia.com/releases/"

Add dependencies:

	"com.ansvia.graph" % "blueprints-scala" % "0.0.1"


***[] Robin Sy.***
