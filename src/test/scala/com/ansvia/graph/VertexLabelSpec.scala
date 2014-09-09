package com.ansvia.graph

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.thinkaurelius.titan.core.{TitanVertex, TitanGraph}
import com.ansvia.graph.testing.model.{SimpleDbo, Animal}
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.util.wrappers.id.{IdVertex, IdGraph}

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

    val HUMAN = "human"
    val ANIMAL = "animal"

    class Ctx extends Scope {


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
    class Ctx3 extends Scope {
        private val titanDb = db.asInstanceOf[TitanGraph]
        val mgmt = titanDb.getManagementSystem
        if (!mgmt.containsRelationType(IdGraph.ID)){
            val __id = mgmt.makePropertyKey(IdGraph.ID).dataType(classOf[java.lang.String]).make()
            mgmt.buildIndex("IDGraphId", classOf[Vertex]).addKey(__id).unique().buildCompositeIndex()
            mgmt.commit()
        }
        implicit val idGraphTitanDb = new IdGraph(titanDb, true, false)
    }


    "When using Titan backed db we" should {
        "be able to create vertex with label" in new Ctx {
            import TitanDbWrapper._

            humanV = SimpleDbo("unyil", "").saveWithLabel(HUMAN)
            animalV = Animal("cat").saveWithLabel(ANIMAL)

            // using string
            animal2V = Animal("lion").saveWithLabel(ANIMAL)

            val _humanV:TitanVertex = titanDb.getVertex(humanV.getId).asInstanceOf[TitanVertex]
            val _animalV:TitanVertex = titanDb.getVertex(animalV.getId).asInstanceOf[TitanVertex]
            val _animal2V:TitanVertex = titanDb.getVertex(animal2V.getId).asInstanceOf[TitanVertex]
            _humanV.getLabel must_== HUMAN
            _animalV.getLabel must_== ANIMAL
            _animal2V.getLabel must_== ANIMAL
        }
        "be able to create vertex with label directly to TitanGraph" in new Ctx2 {

            val v = titanDb.addVertexWithLabel(ANIMAL)
            titanDb.commit()
            val v2:TitanVertex = titanDb.getVertex(v.getId).asInstanceOf[TitanVertex]
            v2.getLabel must_== ANIMAL
        }
        "be able to working using IdGraph wrapper" in new Ctx3 {
            import IdGraphTitanDbWrapper._

            val v = Animal("bear").saveWithLabel(ANIMAL)

            idGraphTitanDb.commit()

            val v2:TitanVertex = idGraphTitanDb.getVertex(v.getId)
                .asInstanceOf[IdVertex].getBaseVertex.asInstanceOf[TitanVertex]

            v2 must_!= null
            v2.getLabel must_== ANIMAL
        }
        "be able to using DbObject" in new Ctx3 {
            import IdGraphTitanDbWrapper._
            import com.ansvia.graph.BlueprintsWrapper._

            val v = Animal("bear").saveWithLabel(ANIMAL)

            idGraphTitanDb.commit()

            v.toCC[Animal] must_!= None
            v.toCC[Animal].get.name must_== "bear"
        }
        "saved vertex has id immediately" in new Ctx3 {
            import IdGraphTitanDbWrapper._
            import com.ansvia.graph.BlueprintsWrapper._

            val v = Animal("tiger").saveWithLabel(ANIMAL)

            v.getId must_!= null
        }
        "get vertex from dbo after save" in new Ctx3 {
            import IdGraphTitanDbWrapper._
            import com.ansvia.graph.BlueprintsWrapper._

            val tiger = Animal("tiger")

            val v = tiger.saveWithLabel(ANIMAL)

            tiger.getVertex must_== v
            tiger.name must_== "tiger"
        }
        "save with label via transaction" in new Ctx3 {
            import IdGraphTitanDbWrapper._
            import com.ansvia.graph.BlueprintsWrapper._

            val id =
            idGraphTitanDb.transact { trx =>
                val tiger = Animal("tiger")

                val v = tiger.saveWithLabelTx(ANIMAL, trx)

                v.getId
            }

            idGraphTitanDb.commit()

            val v = idGraphTitanDb.getVertex(id)

            v must_!= null

            val tiger = v.toCC[Animal].get

            tiger.getVertex must_== v
            tiger.name must_== "tiger"

            // can reload using transaction
            idGraphTitanDb.transact { trx =>
                tiger.reload()(trx) must be not throwAn[Exception]
            }

        }
        "add vertex with label via IdGraph" in new Ctx3 {
            import IdGraphTitanDbWrapper._

            val v = idGraphTitanDb.addVertexWithLabel(ANIMAL)

            v.asInstanceOf[IdVertex].getBaseVertex.asInstanceOf[TitanVertex].getLabel must_== ANIMAL
        }
    }
}
