
package com.ansvia.graph

import com.tinkerpop.blueprints._
import com.ansvia.graph.Exc.{BlueprintsScalaException, NotBoundException}


trait IDGetter[IDType] {
    def isSaved:Boolean
    def getVertex:Vertex

    def getId:IDType = {
        if (!isSaved)
            throw NotBoundException("object %s not saved yet".format(this))
        getVertex.getId.asInstanceOf[IDType]
    }
}
