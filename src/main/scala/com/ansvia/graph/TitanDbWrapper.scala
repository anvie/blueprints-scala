package com.ansvia.graph

import com.tinkerpop.blueprints.Vertex
import com.ansvia.graph.BlueprintsWrapper.{IdDbObject, DbObject}
import com.thinkaurelius.titan.core.{VertexLabel, TitanGraph, TitanTransaction}
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph
import com.thinkaurelius.titan.core.schema.EdgeLabelMaker
import com.thinkaurelius.titan.graphdb.types.StandardEdgeLabelMaker


/**
 * Author: robin
 * Date: 9/1/14
 * Time: 12:46 PM
 *
 */

object TitanDbWrapper extends Helpers {


    class TitanDbWrapper(db:TitanGraph) extends DbWrapper(db){
        def saveWithLabel[T: Manifest](cc:T, label:VertexLabel):Vertex = {
            val (o, _new) = {
                cc match {
                    case dbo:DbObject if dbo.isSaved =>
                        (db.getVertex(dbo.getVertex.getId), false)
                    case _ =>
                        (db.addVertex(label), true)
                }
            }

            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], o, _new)

            cc match {
                case ccDbo:DbObject =>
                    ccDbo.__save__(elm)
                case _ =>
            }
            elm
        }

        def saveWithLabel[T: Manifest](cc:T, label:String):Vertex = {
            val lbl = db.getVertexLabel(label)
            require(lbl != null, "Unknown vertex label: " + label)
            saveWithLabel(cc, lbl)
        }

        def transact[T](f: (TitanTransaction) => T):T = {
            val trx = db.newTransaction()
            try {
                val rv = f(trx)
                trx.commit()
                rv
            }catch{
                case e:IllegalArgumentException =>
                    throw e
                case e:Exception =>
                    e.printStackTrace()
                    trx.rollback()
                    throw e
            }
        }
    }


    class TitanDbObjectWrapper(dbo:DbObject){

        implicit private def transactionWrapper(trx:TitanTransaction) = new TitanTransactionDbWrapper(trx)


        def saveWithLabelTx(label:VertexLabel)(implicit db:TitanTransaction):Vertex = {
            val v = db.saveWithLabel(dbo, label)
            dbo.setVertex(v)
            v
        }

        def saveWithLabelTx(label:String)(implicit db:TitanTransaction):Vertex = {
            val lbl = db.getVertexLabel(label)
            assert(lbl != null, "unknown label: " + label)
            saveWithLabelTx(lbl)
        }

        def saveWithLabel(label:VertexLabel)(implicit db:TitanGraph):Vertex = {
            val v = db.saveWithLabel(dbo, label)
            dbo.setVertex(v)


            dbo match {
                case iddbo:IdDbObject[_] =>
                    iddbo.setId(v.getId.asInstanceOf[iddbo.idType])
                case _ =>
            }

            v
        }

        def saveWithLabel(label:String)(implicit db:TitanGraph):Vertex = {
            val lbl = db.getVertexLabel(label)
            assert(lbl != null, "unknown label: " + label)
            saveWithLabel(lbl)
        }


    }

    implicit def dbWrapper(db:TitanGraph):TitanDbWrapper = new TitanDbWrapper(db)
    implicit def titanDbObjectWrapper(dbo:DbObject):TitanDbObjectWrapper =
        new TitanDbObjectWrapper(dbo)

}



object IdGraphTitanDbWrapper extends Helpers {

    import TitanDbWrapper._

    class IdGraphTitanDbObjectWrapper(dbo:DbObject){

        import com.tinkerpop.blueprints.util.wrappers.id.IdGraph

        protected class IdVertex(v:Vertex, db:IdGraph[TitanGraph])
            extends com.tinkerpop.blueprints.util.wrappers.id.IdVertex(v, db)

        def saveWithLabel(label:String)(implicit db:IdGraph[TitanGraph]):IdVertex = {
            val lbl = db.getBaseGraph.getVertexLabel(label)

            assert(lbl != null, "unknown label: " + label)

            saveWithLabel(lbl)
        }


        def saveWithLabel(label:VertexLabel)(implicit db:IdGraph[TitanGraph]):IdVertex = {
            val v:Vertex = db.getBaseGraph.saveWithLabel(dbo, label)

            val id = db.getVertexIdFactory.createId()

            v.setProperty(IdGraph.ID, id)


            val rv = new IdVertex(v, db)

            dbo.setVertex(rv)

            dbo match {
                case iddbo:IdDbObject[_] =>
                    iddbo.setId(id.asInstanceOf[iddbo.idType])
                case _ =>
            }

            rv
        }

    }

    implicit def idTitanDbWrapper(db:IdGraph[TitanGraph]):TitanDbWrapper = new TitanDbWrapper(db.getBaseGraph)
    implicit def idGraphTitanDbObjectWrapper(dbo:DbObject):IdGraphTitanDbObjectWrapper =
        new IdGraphTitanDbObjectWrapper(dbo)
}


private[graph] trait Helpers {


    class TitanTransactionDbWrapper(db:TitanTransaction) extends DbWrapper(db){
        def saveWithLabel[T: Manifest](cc:T, label:VertexLabel):Vertex = {
            val (o, _new) = {
                cc match {
                    case dbo:DbObject if dbo.isSaved =>
                        (db.getVertex(dbo.getVertex.getId), false)
                    case _ =>
                        (db.addVertex(label), true)
                }
            }

            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], o, _new)

            cc match {
                case ccDbo:DbObject =>
                    ccDbo.__save__(elm)
                case _ =>
            }
            elm
        }

    }

    implicit def edgeLabelMakerWrapper(elm:EdgeLabelMaker) = elm.asInstanceOf[StandardEdgeLabelMaker]


}

