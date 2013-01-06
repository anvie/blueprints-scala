package com.ansvia.graph

/**
 * Copyright (C) 2011-2013 Ansvia Inc.
 * Author: robin
 * Date: 1/7/13
 * 
 */
object Exc {
    class BlueprintsScalaException(msg:String) extends Exception(msg)
    case class NotBoundException(msg:String) extends BlueprintsScalaException(msg)
}
