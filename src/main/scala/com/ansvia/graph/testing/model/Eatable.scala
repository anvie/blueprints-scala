package com.ansvia.graph.testing.model

import com.ansvia.graph.annotation.Persistent


/**
* Author: robin
* Date: 1/14/13
* Time: 9:07 PM
*
*/
private[graph] trait Eatable {
    @Persistent var eatable:Boolean = true
}
