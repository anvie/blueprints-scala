package com.ansvia.graph

import org.specs2.mutable.{After, Specification}
import com.tinkerpop.blueprints.Edge
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph

/**
 * Copyright (C) 2011-2012 Ansvia Inc.
 * User: robin
 * Date: 12/31/12
 * Time: 2:12 AM
 * 
 */
class BlueprintsTransactSpec extends Specification {


    import BlueprintsWrapper._

    sequential

    // since tinkergraph doesn't support transactional
    // we using neo4j db for transactional testing
    implicit val db = new Neo4jGraph("/tmp/neo4jdb-test-transact")

    val data = Map(
        "hercules" -> "demigod",
        "nemean" -> "monster",
        "hydra" -> "monster",
        "cerberus" -> "monster"
    )

    val vertices = for((name, kind) <- data) yield {
        val vertex = db.addVertex(null)
        vertex.setProperty("name", name)
        vertex.setProperty("kind", kind)
        (name, vertex)
    }

    val hercules = vertices("hercules")

    // test with property mutator

    val edge1 = hercules -->"battled"--> vertices("nemean") <()
    val edge2 = hercules -->"battled"--> vertices("hydra") <()
    val edge3 = hercules -->"battled"--> vertices("cerberus") <()

    edge1.set("time", 1)
    edge2.set("time", 2)
    edge3.set("time", 12)

    var edge4:Edge = null

    val edge = hercules --> "test" --> vertices("cerberus") <()
    edge.set("timestamp", 1)

    // test success transaction
    transact {
        edge1.set("win", true)

        // test set datetime
        val edge = hercules --> "test" --> vertices("cerberus") <()
        edge.set("timestamp", 1)

        edge4 = edge
    }

    // test fail transaction
    try {
        transact {
            edge2.set("win", true)
            edge3.set("win", false)
            throw new Exception("oops error man!")
        }
    }catch{
        case e:Exception =>
    }finally {

    }

    "Transaction wrapper" should {
        "handle success transaction correctly" in {
            edge1.has("win") must beTrue
        }
        "handle failed transaction correctly #1" in {
            edge2.has("win") must beFalse
        }
        "handle failed transaction correctly #2" in {
            edge3.has("win") must beFalse
        }
        "create edge and set inside transact" in {
            edge4.has("timestamp") must beTrue
        }
        "create edge and set inside transact return expected data" in {
            edge4.get[Int]("timestamp") must beEqualTo(Some(1))
        }

    }

    step {
        db.shutdown()
    }

}
