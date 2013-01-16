package com.ansvia.graph.testing.model

import com.ansvia.graph.annotation.Persistent
import com.ansvia.graph.BlueprintsWrapper.DbObject
import com.tinkerpop.blueprints.Vertex

/**
 * Author: robin
 * Date: 1/16/13
 * Time: 12:15 AM
 * 
 */

trait B {
    @Persistent var b:Int = 0
}

trait C {
    @Persistent var c:Int = 0
}

abstract class A extends DbObject with B with C {
    @Persistent var a:Int = 0
}

case class Complex(x:String) extends A {
//    import com.ansvia.graph.BlueprintsWrapper._

    @Persistent var me = ""

    /**
     * this method called when loading data from database.
     * override this for custom load routine
     * @param vertex vertex object.
     */
    override def __load__(vertex: Vertex) {
        super.__load__(vertex)
//        me = vertex.getOrElse("me", "")
//        a = vertex.getOrElse("a", 0)
//        b = vertex.getOrElse("b", 0)
//        c = vertex.getOrElse("c", 0)
    }
}
