package com.ansvia.graph.testing.model

import com.tinkerpop.blueprints.Vertex
import com.ansvia.graph.BlueprintsWrapper._


/**
* Author: robin
* Date: 1/14/13
* Time: 9:07 PM
*
*/
case class Shark(kind:String) extends SeaFish("blue") with Eatable {
    var lives:String = ""

    /**
     * override this for custom load routine
     * @param vertex vertex object.
     */
    override def __load__(vertex: Vertex) {
        super.__load__(vertex)
        lives = vertex.getOrElse("lives", "")
        eatable = vertex.getOrElse("eatable", true)
    }
}
