package com.ansvia.graph.testing.model

import com.ansvia.graph.annotation.Persistent
import com.ansvia.graph.BlueprintsWrapper.DbObject

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

class Complex extends A {

    @Persistent var me = "complex"
}
