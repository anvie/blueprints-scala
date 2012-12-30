package com.ansvia.graph

import org.specs2.mutable.Specification
import com.tinkerpop.blueprints.impls.tg.{TinkerGraphFactory, TinkerGraph}
import com.tinkerpop.blueprints.{Vertex, Direction}
import java.lang.Iterable
import com.tinkerpop.gremlin.java.GremlinPipeline
import org.specs2.execute.Skipped

/**
 * Copyright (C) 2011-2012 Ansvia Inc.
 * Author: robin
 *
 * This example simulating gods graph example from
 * https://github.com/thinkaurelius/titan/wiki/Getting-Started
 */
class BlueprintsWrapperSpec extends Specification {

    import BlueprintsWrapper._
    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._

    // for test case we using simple in-memory tinkergraph db
    // in reality you can use any graph db that support
    // tinkerpop blueprints stack
    implicit val db = TinkerGraphFactory.createTinkerGraph()

    val data = Map(
        "hercules" -> "demigod",
        "jupiter" -> "god",
        "alcmene" -> "human",
        "nemean" -> "monster",
        "hydra" -> "monster",
        "cerberus" -> "monster",
        "pluto" -> "god",
        "neptune" -> "god",
        "sky" -> "location",
        "sea" -> "location",
        "tartarus" -> "location"
    )

    val vertices = for((name, kind) <- data) yield {
        val vertex = db.addVertex(null)
        vertex.setProperty("name", name)
        vertex.setProperty("kind", kind)
        (name, vertex)
    }

    vertices("hercules") --> "father" --> vertices("jupiter")
    vertices("hercules") --> "mother" --> vertices("alcmene")
    vertices("jupiter") <--> "brother" <--> vertices("pluto")
    vertices("jupiter") <--> "brother" <--> vertices("neptune") <--> "brother" <--> vertices("pluto")

    val sea = vertices("sea")
    val jupiter = vertices("jupiter")
    val sky = vertices("sky")

    vertices("neptune") --> "lives" --> sea
    sky <-- "lives" <-- jupiter
    vertices("pluto") --> "lives" --> vertices("tartarus") <-- "lives" <-- vertices("cerberus") <-- "pet" <-- vertices("pluto")

    sequential

    "Blueprints vertex wrapper" should {
        "create edge out" in {
            val father = vertices("hercules").getVertices(Direction.OUT,"father").iterator().next()
            father.getProperty("name") must beEqualTo("jupiter")
        }
        "able to using getter" in {
            vertices("hercules").get[String]("kind").isDefined must beTrue
        }
        "getter not-exist" in {
            vertices("hercules").get[String]("not_exists").isDefined must beFalse
        }
        "able to using getter or else exists" in {
            vertices("hercules").getOrElse[String]("kind", "") must beEqualTo("demigod")
        }
        "able to using getter or else not-exists" in {
            vertices("hercules").getOrElse[String]("sex", "male") must beEqualTo("male")
        }
        "create edge out using getter" in {
            val father = vertices("hercules").getVertices(Direction.OUT,"father").iterator().next()
            father.get[String]("name").get must beEqualTo("jupiter")
        }
        "create edge in" in {
            val father = vertices("jupiter").getVertices(Direction.IN,"father").iterator().next()
            father.get[String]("name").get must beEqualTo("hercules")
        }
        "create mutual connection" in {
            val it = vertices("jupiter").getEdges(Direction.BOTH, "brother").iterator()
            val edge1 = it.next()
            val edge2 = it.next()
            edge1.getLabel must beEqualTo(edge2.getLabel)
        }
        "using mutual wrapper #1" in {
            val vx = vertices("jupiter").mutual("brother")
            vx.iterator().next().get[String]("name").get must beEqualTo("pluto")
        }
        "using mutual wrapper #2" in {
            val vx: Iterable[Vertex] = vertices("pluto").mutual("brother")
            vx.iterator().next().get[String]("name").get must beEqualTo("jupiter")
        }
        "dump iterable vertices" in {
            vertices("hercules").getEdges(Direction.OUT).printDump("name")
            true must beTrue
        }
        "long right edges" in {
            val edge = vertices("pluto").getEdges(Direction.OUT, "pet").iterator().next()
            edge.getVertex(Direction.IN).getOrElse[String]("name", "") must beEqualTo("cerberus")
        }
    }

    val hercules = vertices("hercules")

    // test with property mutator

    val edge1 = hercules -->"battled"--> vertices("nemean") <
    val edge2 = hercules -->"battled"--> vertices("hydra") <
    val edge3 = hercules -->"battled"--> vertices("cerberus") <

    edge1.set("time", 1)
    edge2.set("time", 2)
    edge3.set("time", 12)

    "Blueprints gremline pipe" should {
        "do simple query" in {
            val vx = vertices("tartarus").pipe.in("lives")
            val lst = vx.printDumpGetList("they are lives in tartarus:", "name")
            lst.length must beEqualTo(2)
        }
        "iterate and map both vertices" in {
            val jupiterBrother = jupiter.pipe.out("brother").iterator()
                .toList.map( v =>  v.getOrElse[String]("name", "") )
            jupiterBrother.length == 2 &&
            jupiterBrother(0) == "neptune" &&
            jupiterBrother(1) == "pluto" must beTrue
        }
        "get count of out edges" in {
            hercules.pipe.outE("battled").count() must beEqualTo(3)
        }
        "using currying filter / gremlin pipe wrapper" in {
            val vx = hercules.pipe.outE("battled").wrap.filter { edge =>
                edge.getOrElse[Int]("time", 0).toString.toInt > 5
            }
            vx.iterator().next().getOrElse[Int]("time", 0) must beEqualTo(12)
        }
    }
}
