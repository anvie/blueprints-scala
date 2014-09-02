package com.ansvia.graph

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.thinkaurelius.titan.core.{TitanVertex, TitanGraph}
import com.ansvia.graph.testing.model.{SimpleDbo, User, Animal}
import com.tinkerpop.blueprints.Vertex

/**
 * Author: robin
 * Date: 9/1/14
 * Time: 3:21 PM
 *
 *
 * Titan only specific label feature test
 */


class VertexLabelSpec extends Specification with TitanBackedDb {

    import TitanDbWrapper._

    sequential

    class Ctx extends Scope {

        val HUMAN = "human"
        val ANIMAL = "animal"

        implicit val titanDb = db.asInstanceOf[TitanGraph]

        var humanV:Vertex = _
        var animalV:Vertex = _
        var animal2V:Vertex = _

        val trx = titanDb.getManagementSystem
//        titanDb.transact { implicit trx =>
            trx.makeVertexLabel(HUMAN).make()
            trx.makeVertexLabel(ANIMAL).make()
        trx.commit()
//        }
    }

    class Ctx2 extends Scope {
        implicit val titanDb = db.asInstanceOf[TitanGraph]
    }


    "When using Titan backed db we" should {
        "be able to create vertex with label" in new Ctx {
            humanV = SimpleDbo("unyil", "").saveWithLabel(HUMAN)
            animalV = Animal("cat").saveWithLabel(ANIMAL)

            // using string
            animal2V = Animal("lion").saveWithLabel("animal")

            val _humanV:TitanVertex = titanDb.getVertex(humanV.getId).asInstanceOf[TitanVertex]
            val _animalV:TitanVertex = titanDb.getVertex(animalV.getId).asInstanceOf[TitanVertex]
            val _animal2V:TitanVertex = titanDb.getVertex(animal2V.getId).asInstanceOf[TitanVertex]
            _humanV.getLabel must_== "human"
            _animalV.getLabel must_== "animal"
            _animal2V.getLabel must_== "animal"
        }
        "be able to create vertex with label directly to TitanGraph" in new Ctx2 {
            val v = titanDb.addVertexWithLabel("animal")
            titanDb.commit()
            val v2:TitanVertex = titanDb.getVertex(v.getId).asInstanceOf[TitanVertex]
            v2.getLabel must_== "animal"
        }
    }
}
