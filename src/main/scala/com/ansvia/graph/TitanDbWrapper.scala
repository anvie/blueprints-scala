package com.ansvia.graph

import com.tinkerpop.blueprints.{Graph, Vertex}
import com.ansvia.graph.BlueprintsWrapper.DbObject
import com.thinkaurelius.titan.core.{VertexLabel, TitanGraph, TitanTransaction}

/**
 * Author: robin
 * Date: 9/1/14
 * Time: 12:46 PM
 *
 */

class TitanDbWrapper(db:TitanGraph) extends DbWrapper(db){
    def saveWithLabel[T: Manifest](cc:T, label:VertexLabel):Vertex = {
        val (o, _new) = {
            cc match {
                case dbo:DbObject if dbo.isSaved =>
                    (db.getVertex(dbo.getVertex.getId), false)
//                case dbo:DbObject if !dbo.isSaved =>
//                    (db.addVertex(null), true)
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

    def transact[T](f: (TitanTransaction) => T):T = {
        val trx = db.newTransaction()
        try {
            val rv = f(trx)
            trx.commit()
            rv
        }catch{
            case e:Exception =>
                trx.rollback()
                throw e
        }
    }
}

class TitanDbObjectWrapper(dbo:DbObject){

    def saveWithLabel(label:VertexLabel)(implicit db:TitanTransaction):Vertex = {
        val v = db.addVertexWithLabel(label)
        dbo.setVertex(v)
        v
    }

}

object TitanDbWrapper {


    implicit def dbWrapper(db:TitanGraph) = new TitanDbWrapper(db)

    implicit def titanDbObjectWrapper(dbo:DbObject) = new TitanDbObjectWrapper(dbo)


}
