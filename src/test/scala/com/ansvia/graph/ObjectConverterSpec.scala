package com.ansvia.graph

import org.specs2.mutable.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import com.tinkerpop.blueprints.Vertex
import com.ansvia.graph.BlueprintsWrapper._

/**
 * Copyright (C) 2011-2012 Ansvia Inc.
 * User: robin
 * Date: 12/31/12
 * Time: 5:21 AM
 * 
 */
class ObjectConverterSpec extends Specification {

    import com.ansvia.graph.testing.model._

    implicit val db = TinkerGraphFactory.createTinkerGraph()

    val v = db.addVertex(null)
    v.set("name", "robin")
    v.set("age", 25)
    v.set("_class_", "com.ansvia.graph.testing.model.User")

    val vtcc1 = ObjectConverter.toCC[User](v)

    val ccU = User("gondez", 35)

    val v2 = db.save(ccU)

    val vtcc2 = ObjectConverter.toCC[User](v2)

    val v3 = Motor("Honda").save()
    val v4o = Animal("cat")
    v4o.age = 5
    v4o.kind = "mamalia"
    val v4 = v4o.save()
    val v4ob = v4.toCC[Animal].get

    v3 --> "hit" --> v4ob


    val nemoDraft = SeaFish("yellow")
    nemoDraft.name = "nemo"
    val nemo = nemoDraft.save().toCC[SeaFish].get

    val sharkDraft = Shark("Hammer head")
    sharkDraft.name = "the killer"
    sharkDraft.lives = "Atlantica"
    sharkDraft.eatable = false
    val shark = sharkDraft.save().toCC[Shark].get


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
        "work with non param var get parameter" in {
            v4ob.name must beEqualTo("cat")
        }
        "work with non param var get inner variable #1" in {
            v4ob.age must beEqualTo(5)
        }
        "work with non param var get inner variable #2" in {
            v4ob.kind must beEqualTo("mamalia")
        }
    }

    "DbObject inherited class" should {
        "save directly using .save()" in {
            v3.isInstanceOf[Vertex] must beTrue
        }
        "has expected field" in {
            v3.has("mark") must beTrue
        }
        "get back saved field data" in {
            v3.get[String]("mark").getOrElse("mark", "esemka") must beEqualTo("Honda")
        }
        "deserializable" in {
            v3.toCC[Motor].isDefined must beTrue
        }
        "has expected data in deserialized object" in {
            v3.toCC[Motor].get.mark must beEqualTo("Honda")
        }
        "able to get raw vertex from case class" in {
            v4ob.getVertex must beEqualTo(v4)
        }
        "make relation DbObject to DbObject #1" in {
            v4ob.getVertex.pipe.inFirst("hit").isDefined must beTrue
        }
        "make relation DbObject to DbObject #2" in {
            v4ob.getVertex.pipe.inFirst("hit").get.toCC[Motor].get.mark must beEqualTo("Honda")
        }
        "unsaved DbObject return false in isSaved" in {
            v4o.isSaved must beFalse
        }
        "saved DbObject return true in isSaved" in {
            v4ob.isSaved must beTrue
        }
        "class contain lazy or val should not raising invocation error" in {
            ContainLazy(1).save() must not equalTo(null)
        }
        "access upper variable #1" in {
            nemo.name must beEqualTo("nemo")
        }
        "access upper variable #2" in {
            nemo.color must beEqualTo("yellow")
        }
        "access upper-upper variable #1" in {
            shark.color must beEqualTo("blue")
        }
        "access upper-upper variable #2" in {
            shark.name must beEqualTo("the killer")
        }
        "access upper-upper variable #3" in {
            shark.kind must beEqualTo("Hammer head")
        }
        "access lower variable via loader from upper-upper" in {
            shark.lives must beEqualTo("Atlantica")
        }
        "access trait variable" in {
            shark.eatable must beFalse
        }
    }

}



