package com.ansvia.graph.testing.model

import com.tinkerpop.blueprints.Vertex
import com.ansvia.graph.BlueprintsWrapper._
import com.ansvia.graph.annotation.Persistent


/**
* Author: robin
* Date: 1/14/13
* Time: 9:07 PM
*
*/
private[graph] abstract class Fish extends DbObject {
    @Persistent var name:String = ""

//    /**
//     * override this for custom load routine
//     * @param vertex vertex object.
//     */
//    override def __load__(vertex: Vertex) {
//        super.__load__(vertex)
//        name = vertex.getOrElse("name", "")
//    }
}
