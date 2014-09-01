package com.ansvia.graph

import com.tinkerpop.blueprints.{Graph, Vertex}
import com.ansvia.graph.BlueprintsWrapper.DbObject

/**
 * Author: robin
 * Date: 9/1/14
 * Time: 12:47 PM
 *
 */
class DbWrapper(db:Graph) {

    def save[T:Manifest](cc:T):Vertex = {
        val (o, _new) = {
            cc match {
                case dbo:DbObject if dbo.isSaved =>
                    (db.getVertex(dbo.getVertex.getId), false)
//                case dbo:DbObject if !dbo.isSaved =>
//                    (db.addVertex(null), true)
                case _ =>
                    (db.addVertex(null), true)
            }
        }

        val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], o, _new)

        cc match {
            case ccDbo:DbObject =>
                ccDbo.__save__(elm)
            //                    val kv = ccDbo.__save__()
            //                    for ( (k, v) <- kv ){
            //
            //                        // only set if different/new
            //                        if(elm.getOrElse(k,null) != v)
            //                            elm.set(k, v)
            //
            //                    }
            case _ =>
        }
        elm
    }

    def delete[T:Manifest](cc:T):Unit = {
        cc match {
            case dbo:DbObject if dbo.isSaved =>
                db.removeVertex(dbo.getVertex)
            case _ =>
        }
    }
}

object StdDbWrapper {
    implicit def dbWrapper(db:Graph) = new DbWrapper(db)
}

