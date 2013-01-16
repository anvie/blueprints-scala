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
case class Shark(kind:String) extends SeaFish("blue") with Eatable {

    @Persistent var lives:String = ""
    @Persistent var hungry:Boolean = false

    var children:Int = 0
    var animalProtected = false

    /**
     * override this for custom load routine
     * @param vertex vertex object.
     */
    override def __load__(vertex: Vertex) {
        super.__load__(vertex)
//        lives = vertex.getOrElse("lives", "")
        eatable = vertex.getOrElse("eatable", true)
        children = vertex.getOrElse("children", 0)
        animalProtected = vertex.getOrElse("protected", false)
    }

    /**
     * this method called before saving into database,
     * override this for custom kv properties.
     * all return from this method will be saved.
     * by default this is just return empty map.
     * @return
     */
    override def __save__() = {
        Map("protected" -> animalProtected)
    }
}
