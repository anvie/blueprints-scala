package com.ansvia.graph

import com.tinkerpop.blueprints.{Edge, Graph, Vertex}
import com.ansvia.graph.BlueprintsWrapper.{DbObject, VertexWrapper}


/**
 * Edge wrapper on arrow chain.
 * This wrapper automatic used via VertexWrapper.
 * @param vertex vertex
 * @param label label
 * @param db database
 */
private[graph] case class EdgeWrapper(var vertex:Vertex, var label:String, db:Graph) extends Wrapper {
    private var lastEdge:Edge = null
    var prev:Option[VertexWrapper] = None

    def -->(inV:Vertex):VertexWrapper = {
        lastEdge = db.addEdge(null, vertex, inV, label)

        // for performance reason
        // we using previous object if any

        val p = prev.getOrElse {
            VertexWrapper(inV, label, db)
        }

        p.prev = Some(this)
        p.vertex = inV
        p
    }

    def <--(outV:Vertex):VertexWrapper = {
        lastEdge = db.addEdge(null, outV, vertex, label)

        // for performance reason
        // we using previous object if any

        val p = prev.getOrElse {
            VertexWrapper(outV, label, db)
        }
        p.prev = Some(this)
        p.vertex = outV
        p
    }

    def -->(o:DbObject):VertexWrapper = {
        this.-->(o.getVertex)
    }

    def <():Edge = this.lastEdge
}
