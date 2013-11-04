package com.ansvia.graph.testing.model

import com.ansvia.graph.AbstractIDGetter
import com.ansvia.graph.BlueprintsWrapper.DbObject

/**
 * Author: robin
 * Date: 11/4/13
 * Time: 1:23 PM
 *
 */
private[graph] case class ModelUsingAbstractIDGetter(name:String) extends DbObject with AbstractIDGetter[String] {
    def getId = "xxx"
}
