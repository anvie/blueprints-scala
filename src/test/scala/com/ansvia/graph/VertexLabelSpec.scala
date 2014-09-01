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

    sequential

    class Ctx extends Scope {
        import TitanDbWrapper._

        implicit val titanDb = db.asInstanceOf[TitanGraph]

        var humanV:Vertex = _
        var animalV:Vertex = _

        titanDb.transact { implicit trx =>
            val HUMAN = trx.makeVertexLabel("human").make()
            val ANIMAL = trx.makeVertexLabel("animal").make()

            humanV = SimpleDbo("unyil", "").saveWithLabel(HUMAN)
            animalV = Animal("cat").saveWithLabel(ANIMAL)
        }
    }

    "When using Titan backed db we" should {
        "be able to create vertex with label" in new Ctx {
            val _humanV:TitanVertex = titanDb.getVertex(humanV.getId).asInstanceOf[TitanVertex]
            val _animalV:TitanVertex = titanDb.getVertex(animalV.getId).asInstanceOf[TitanVertex]
            _humanV.getLabel must_== "human"
            _animalV.getLabel must_== "animal"
        }
    }
}
