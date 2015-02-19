package com.ansvia.graph.testing.model

import com.ansvia.graph.BlueprintsWrapper.{IdDbObject, DbObject}
import com.ansvia.graph.IDGetter

/**
 * Author: robin
 * Date: 2/17/13
 * Time: 9:54 PM
 *
 * For test purpose.
 */
private[graph] case class SimpleDbo(a:String,var b:String) extends DbObject with IDGetter[String]
private[graph] case class SimpleDboLong(a:String,var b:String) extends DbObject with IDGetter[Long]
private[graph] case class IdSimpleDbo(a:String,var b:String) extends IdDbObject[String]
private[graph] case class IdSimpleDboLong(a:String,var b:String) extends IdDbObject[Long]
private[graph] case class IdSimpleDboOption(opt: Option[String] = None, a:String,var b:String) extends IdDbObject[Long]
private[graph] case class IdSimpleDboVarOption(var opt: Option[String] = None, a:String,var b:String) extends IdDbObject[Long]

