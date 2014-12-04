package com.ansvia.graph

import org.specs2.mutable.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import org.specs2.specification.Scope
import com.tinkerpop.blueprints.{Direction, Edge, Vertex}

/**
 * Author: robin
 * Date: 2/24/14
 * Time: 1:07 AM
 *
 */
class GremlinWrapperSpec extends Specification {

    import scala.collection.JavaConversions._
    import com.ansvia.graph.BlueprintsWrapper._
    import gremlin._

    implicit val db = TinkerGraphFactory.createTinkerGraph()

    class Ctx extends Scope {

        val follow = "follow"

        val name1 = "unyil"
        val name2 = "usrok"
        val name3 = "ogah"

        val v1 = db.addVertex(null)
        val v2 = db.addVertex(null)
        val v3 = db.addVertex(null)

        v1.setProperty("name", name1)
        v2.setProperty("name", name2)
        v3.setProperty("name", name3)

        v1.setProperty("age", 17)
        v2.setProperty("age", 22)
        v3.setProperty("age", 19)

        v1 --> follow --> v2
        v1 --> follow --> v3
    }

    "gremlin wrapper" should {
        "wrap transform" in new Ctx {
            v1.pipe.out(follow).transform { (v:Vertex) =>
                v.getOrElse("name","")
            }.iterator().toList must contain(name2, name3)
        }
        "wrap side effect" in new Ctx {
            v1.pipe.out(follow).sideEffect { (v:Vertex) =>
                v.setProperty("name", v.getProperty[String]("name") + "!")
            }
            val vx = db.getVertex(v1.getId)
            vx.get("name") must_== Some(name1)
        }
        "wrap order" in new Ctx {
            v1.pipe.out(follow).order { (a:Vertex, b:Vertex) =>
                a.getProperty[java.lang.Integer]("age").compareTo(b.getProperty[java.lang.Integer]("age"))
            }.iterator().toList must contain(v3,v2).inOrder
        }
        "wrap filter vertex" in new Ctx {
            v1.pipe.out(follow).filter { (v:Vertex) =>
                v.get[String]("name") == Some(name3)
            }.iterator().toList must beEqualTo(List(v3))
        }
        "wrap filter edge" in new Ctx {
            v1.pipe.outE(follow).filter { (e:Edge) =>
                e.getVertex(Direction.IN).get[String]("name") == Some(name3)
            }.inV().iterator().toList must beEqualTo(List(v3))
        }
    }
}
