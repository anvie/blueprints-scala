package com.ansvia.graph.util

/**
 * Utility object used for fetching callers classLoader
 */
object CallersContext {

  def fetchDefaultClassLoader = {
    var context: Array[Class[_]] = null
    new SecurityManager {
      context = getClassContext()
    }

    if(context.length > 2) context(2).getClassLoader else null
  }
}