package com.ansvia.graph

import org.specs2.mutable.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory

/**
 * Copyright (C) 2011-2012 Ansvia Inc.
 * User: robin
 * Date: 12/31/12
 * Time: 5:21 AM
 * 
 */
class ObjectConverterSpec extends Specification {

    import com.ansvia.graph.BlueprintsWrapper._

    implicit val db = TinkerGraphFactory.createTinkerGraph()

    val v = db.addVertex(null)
    v.set("name", "robin")
    v.set("age", 25)
    v.set("_class_", "com.ansvia.graph.User")

    val vtcc1 = ObjectConverter.toCC[User](v)

    val ccU = User("gondez", 35)

    val v2 = db.save(ccU)

    val vtcc2 = ObjectConverter.toCC[User](v2)

    "Object converter" should {
        "convert vertex to case class #1" in {
            vtcc1.isDefined must beTrue
        }
        "convert vertex to case class #2" in {
            vtcc1.get.name must beEqualTo("robin")
        }
        "convert vertex to case class #3" in {
            vtcc1.get.age must beEqualTo(25)
        }
        "convert case class to vertex #1" in {
            v2.get("_class_").isDefined must beTrue
        }
        "convert case class to vertex #3" in {
            v2.get[String]("_class_").get must beEqualTo("com.ansvia.graph.User")
        }
        "convert back from vertex to case class #1" in {
            vtcc2.isDefined must beTrue
        }
        "convert back from vertex to case class #2" in {
            vtcc2.get.name must beEqualTo("gondez")
        }
        "convert back from vertex to case class #3" in {
            vtcc2.get.age must beEqualTo(35)
        }
    }

}

case class User(name:String, age:Int)
