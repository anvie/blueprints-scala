package com.ansvia.graph.testing.model

import com.tinkerpop.blueprints.Vertex
import com.ansvia.graph.BlueprintsWrapper._


/**
* Author: robin
* Date: 1/14/13
* Time: 9:07 PM
*
*/
case class Animal(name:String) extends DbObject {
    var age:Int = 0
    var kind:String = ""

    /**
     * override  this for custom load routine
     * @param vertex vertex object.
     */
    override def __load__(vertex: Vertex) {
        super.__load__(vertex)
        age = vertex.getOrElse[Int]("age", 0)
        kind = vertex.getOrElse[String]("kind", "")
    }
}
