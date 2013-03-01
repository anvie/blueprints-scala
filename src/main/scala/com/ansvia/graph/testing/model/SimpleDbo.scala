package com.ansvia.graph.testing.model

import com.ansvia.graph.BlueprintsWrapper.DbObject
import com.ansvia.graph.IDGetter

/**
 * Author: robin
 * Date: 2/17/13
 * Time: 9:54 PM
 *
 */
private[graph] case class SimpleDbo(a:String,var b:String) extends DbObject with IDGetter[String]

