package com.ansvia.graph

import com.ansvia.graph.BlueprintsWrapper._
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import org.specs2.mutable.Specification

/**
 * This specification simulates behavior of converting vertex with use of different classloader.
 * Author: pdolega
 */
class DifferentClassLoaderSpec extends Specification {

    "Blueprints vertex wrapper" should {
        "attempt to create case class if specified class loader" in {
            val myClassLoader = new CustomClassLoader(getClass.getClassLoader)

            implicit val db = TinkerGraphFactory.createTinkerGraph()
            db.save(Person(name = "Keyser Soze")).toCC[Person](myClassLoader) must throwA(new ClassLoaderUsedException(classOf[Person].getName))
        }

        "load case class with parent classloader if not specified otherwise" in {
            implicit val db = TinkerGraphFactory.createTinkerGraph()
            val herkules = db.save(Person(name = "Tony Stark")).toCC[Person]

            herkules must beEqualTo(Some(Person(name = "Tony Stark")))
        }
    }
}

class CustomClassLoader(val parentClassLoader: ClassLoader) extends ClassLoader {
    override def loadClass(name: String, resolve: Boolean): Class[_] = {
        throw new ClassLoaderUsedException(name)
    }
}

class ClassLoaderUsedException(message: String) extends Exception(message)

case class Person(name: String)
