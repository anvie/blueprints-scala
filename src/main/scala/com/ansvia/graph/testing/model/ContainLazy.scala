package com.ansvia.graph.testing.model

import com.ansvia.graph.BlueprintsWrapper._


/**
* Author: robin
* Date: 1/14/13
* Time: 9:07 PM
*
*/
case class ContainLazy(test:Long) extends DbObject {
    lazy val x = {
        2
    }
    val z = 3
}
