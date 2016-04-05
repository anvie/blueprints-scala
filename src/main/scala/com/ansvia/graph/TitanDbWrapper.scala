package com.ansvia.graph

import com.ansvia.graph.BlueprintsWrapper.{DbObject, IdDbObject}
import com.thinkaurelius.titan.core.schema.EdgeLabelMaker
import com.thinkaurelius.titan.core.{TitanGraph, TitanTransaction, VertexLabel}
import com.thinkaurelius.titan.graphdb.types.StandardEdgeLabelMaker
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph.IdFactory
import com.tinkerpop.blueprints.util.wrappers.id.{IdGraph, IdVertex}
import com.tinkerpop.blueprints.{KeyIndexableGraph, Vertex}

import scala.language.reflectiveCalls


/**
 * Author: robin
 * Date: 9/1/14
 * Time: 12:46 PM
 *
 */

object TitanDbWrapper extends Helpers {


    class TitanDbWrapper(db:TitanGraph) extends DbWrapper(db){
        def saveWithLabel[T: Manifest](cc:T, label:VertexLabel):Vertex = {
            val (v, _new) = {
                cc match {
                    case dbo:DbObject if dbo.isSaved =>
                        (db.getVertex(dbo.getVertex.getId), false)
                    case _ =>
                        (db.addVertex(label), true)
                }
            }

            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], v, _new)

            cc match {
                case ccDbo:DbObject =>
                    ccDbo.setVertex(elm)
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
//                case e:Exception =>
//                    e.printStackTrace()
//                    try {
//                        trx.rollback()
//                    }catch{
//                        case e:Throwable =>
//                    }
//                    throw e
            }
        }

        def transactIdGraph[T](idFactory:IdFactory)(f: (IdGraph[TitanTransaction]) => T):T = {
            val trx = new IdGraph(db.newTransaction(), true, false)
            trx.setVertexIdFactory(idFactory)
            try {
                val rv = f(trx)
                trx.commit()
                rv
            }catch{
                case e:IllegalArgumentException =>
                    throw e
//                case e:Exception =>
//                    e.printStackTrace()
//                    try {
//                        trx.rollback()
//                    }catch{
//                        case e:Throwable =>
//                    }
//                    throw e
            }
        }

    }


    class TitanDbObjectWrapper(dbo:DbObject){

        implicit private def transactionWrapper(trx:TitanTransaction) = new TitanTransactionDbWrapper(trx)


        def saveWithLabelTx(label:VertexLabel)(implicit db:TitanTransaction):Vertex = {
            db.saveWithLabel(dbo, label)
//            dbo.setVertex(v)
//            v
        }

        def saveWithLabelTx(label:String)(implicit db:TitanTransaction):Vertex = {
            val lbl = db.getVertexLabel(label)
            assert(lbl != null, "unknown vertex label: " + label)
            saveWithLabelTx(lbl)
        }

        def saveWithLabel(label:VertexLabel)(implicit db:TitanGraph):Vertex = {
            val v = db.saveWithLabel(dbo, label)
//            dbo.setVertex(v)


            dbo match {
                case iddbo:IdDbObject[_] =>
                    iddbo.setId(v.getId.asInstanceOf[iddbo.idType])
                case _ =>
            }

            v
        }

        def saveWithLabel(label:String)(implicit db:TitanGraph):Vertex = {
            val lbl = db.getVertexLabel(label)
            assert(lbl != null, "unknown vertex label: " + label)
            saveWithLabel(lbl)
        }


    }

    implicit def dbWrapper(db:TitanGraph):TitanDbWrapper = new TitanDbWrapper(db)
    implicit def titanDbObjectWrapper(dbo:DbObject):TitanDbObjectWrapper =
        new TitanDbObjectWrapper(dbo)

}



object IdGraphTitanDbWrapper extends Helpers {

    import TitanDbWrapper._



    protected class _IdVertex(v:Vertex, db:IdGraph[_ <: KeyIndexableGraph])
        extends com.tinkerpop.blueprints.util.wrappers.id.IdVertex(v, db)


    implicit def idTitanDbWrapper(db:IdGraph[TitanGraph]) = new TitanDbWrapper(db.getBaseGraph){


        override def saveWithLabel[T: Manifest](cc: T, label: VertexLabel):IdVertex = {
            val (v, _new) = {
                cc match {
                    case dbo:DbObject if dbo.isSaved =>
                        (db.getBaseGraph.getVertex(dbo.getVertex.getId), false)
                    case _ =>
                        (db.getBaseGraph.addVertex(label), true)
                }
            }

            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], v, _new)
            val idV = new _IdVertex(elm, db)

            cc match {
                case ccDbo:DbObject =>
                    ccDbo.setVertex(idV)
                    ccDbo.__save__(idV)
                case _ =>
            }
//            elm
            idV
        }

        def addVertexWithLabel(label:VertexLabel):Vertex = {
            val rawV = db.getBaseGraph.addVertexWithLabel(label)

            val id = db.getVertexIdFactory.createId()

            rawV.setProperty(IdGraph.ID, id)

            new _IdVertex(rawV, db)
        }

        def addVertexWithLabel(label:String):Vertex = {
            val lbl = db.getBaseGraph.getVertexLabel(label)
            assert(lbl != null, "unknown vertex label: " + label)
            addVertexWithLabel(lbl)
        }

    }


    implicit def titanTransactionWrapper(trx:IdGraph[TitanTransaction]) = new TitanTransactionDbWrapper(trx.getBaseGraph){

        //        protected class IdVertex(v:Vertex, db:IdGraph[TitanTransaction])
        //            extends com.tinkerpop.blueprints.util.wrappers.id.IdVertex(v, db)
        //

        override def saveWithLabel[T: Manifest](cc: T, label: VertexLabel):_IdVertex = {
            val (v, _new) = {
                cc match {
                    case dbo:DbObject if dbo.isSaved =>
                        (trx.getBaseGraph.getVertex(dbo.getVertex.getId), false)
                    case _ =>
                        (trx.getBaseGraph.addVertex(label), true)
                }
            }

            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], v, _new)

            val idV = new _IdVertex(elm, trx)

            cc match {
                case ccDbo:DbObject =>
                    ccDbo.setVertex(idV)
                    ccDbo.__save__(idV)
                case _ =>
            }
//            elm

            idV
        }

        def addVertexWithLabel(label:VertexLabel):_IdVertex = {

            val id = trx.getVertexIdFactory.createId()

            val rawV = trx.getBaseGraph.addVertexWithLabel(label)

            rawV.setProperty(IdGraph.ID, id)

            new _IdVertex(rawV, trx)
        }

        def addVertexWithLabel(label:String):Vertex = {
            val lbl = trx.getBaseGraph.getVertexLabel(label)
            assert(lbl != null, "unknown vertex label: " + label)
            addVertexWithLabel(lbl)
        }

    }


    class IdGraphTitanDbObjectWrapper(dbo:DbObject){


        import com.tinkerpop.blueprints.util.wrappers.id.IdGraph
//
//        protected class IdVertex(v:Vertex, db:IdGraph[TitanGraph])
//            extends com.tinkerpop.blueprints.util.wrappers.id.IdVertex(v, db)


        def saveWithLabel(label:String)(implicit db:IdGraph[TitanGraph]):IdVertex = {
            val lbl = db.getBaseGraph.getVertexLabel(label)

            assert(lbl != null, "unknown vertex label: " + label)

            saveWithLabel(lbl)
        }


        def saveWithLabel(label:VertexLabel)(implicit db:IdGraph[TitanGraph]):IdVertex = {
            val v:IdVertex = db.saveWithLabel(dbo, label)

            val id = db.getVertexIdFactory.createId()

            v.getBaseVertex.setProperty(IdGraph.ID, id)

            dbo match {
                case iddbo:IdDbObject[_] =>
                    iddbo.setId(id.asInstanceOf[iddbo.idType])
                case _ =>
            }

            v
        }

        def saveWithLabelTx(label:VertexLabel, trx:TitanTransaction)(implicit db:IdGraph[TitanGraph]):_IdVertex = {
            val idTrx = new IdGraph(trx, true, false)
//            val trxw = new TitanTransactionDbWrapper(trx)

            val v:_IdVertex = titanTransactionWrapper(idTrx).saveWithLabel(dbo, label)

            val id = db.getVertexIdFactory.createId()

            v.getBaseVertex.setProperty(IdGraph.ID, id)

//            val rv = new _IdVertex(v, db)

//            dbo.setVertex(rv)

            dbo match {
                case iddbo:IdDbObject[_] =>
                    iddbo.setId(id.asInstanceOf[iddbo.idType])
                case _ =>
            }

//            rv
            v
        }

        def saveWithLabelTx(label:String, trx:TitanTransaction)(implicit db:IdGraph[TitanGraph]):_IdVertex = {
            val lbl = trx.getVertexLabel(label)
            assert(lbl != null, "unknown vertex label: " + label)
            saveWithLabelTx(lbl, trx)
        }


    }

    implicit def idGraphTitanDbObjectWrapper(dbo:DbObject):IdGraphTitanDbObjectWrapper =
        new IdGraphTitanDbObjectWrapper(dbo)

}


private[graph] trait Helpers {


    class TitanTransactionDbWrapper(trx:TitanTransaction) extends DbWrapper(trx){

        def saveWithLabel[T: Manifest](cc:T, label:VertexLabel):Vertex = {
            val (v, _new) = {
                cc match {
                    case dbo:DbObject if dbo.isSaved =>
                        (trx.getVertex(dbo.getVertex.getId), false)
                    case _ =>
                        (trx.addVertex(label), true)
                }
            }

            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], v, _new)

            cc match {
                case ccDbo:DbObject =>
                    ccDbo.setVertex(elm)
                    ccDbo.__save__(elm)
                case _ =>
            }
            elm
        }

    }


    implicit def edgeLabelMakerWrapper(elm:EdgeLabelMaker) = elm.asInstanceOf[StandardEdgeLabelMaker]
    implicit def titanTransactionWrapper(trx:TitanTransaction) = new TitanTransactionDbWrapper(trx)


}

