package com.ansvia.graph

/**
 * Copyright (C) 2011-2012 Ansvia Inc.
 * User: robin
 * Date: 12/31/12
 * Time: 5:21 AM
 * 
 */
import collection.JavaConversions._
import com.tinkerpop.blueprints.{Vertex, Element}
import util.CaseClassDeserializer
import com.ansvia.graph.BlueprintsWrapper.DbObject

object ObjectConverter {

//    class Persistent extends scala.annotation.Annotation

//    import BlueprintsWrapper._

    /**
     * this name will be used to store the class name of
     * the serialized case class that will be verified
     * in deserialization
     */
    val CLASS_PROPERTY_NAME = "_class_"

    /**
     * serializes a given case class into a Node instance
     * for null values not property will be set
     */
    def serialize[T <: Element](cc: AnyRef, pc: Element): T = {
        CaseClassDeserializer.serialize(cc).foreach {
            case (name, null) =>
            case (name, value) => pc.setProperty(name, value)
        }
        pc.setProperty(CLASS_PROPERTY_NAME, cc.getClass.getName)



        // save non case class accessor

        pc.asInstanceOf[T]
    }

    /**
     * conditional case class deserialization
     * Some(T) if possible
     * None if not
     */
    def toCC[T: Manifest](pc: Element): Option[T] =
        _toCCPossible(pc) match {
            case Some(serializedClass) =>

                val kv = for (k <- pc.getPropertyKeys; v = pc.getProperty(k)) yield (k -> v)

                val o = CaseClassDeserializer.deserialize[T](serializedClass, kv.toMap)

                if (o.isInstanceOf[DbObject]){
                    o.asInstanceOf[DbObject].__load__(pc.asInstanceOf[Vertex])
                }

                Some(o)

            case _ => None
        }

    private def _toCCPossible[T: Manifest](pc: Element): Option[Class[_]] = {
        val cpn = pc.getProperty(CLASS_PROPERTY_NAME).toString
        val c = Class.forName(cpn)
        if (manifest[T].erasure.isAssignableFrom(c))
            Some(c)
        else
            None
    }

    /**
     * only checks if this property container has been serialized
     * with T
     */
    def toCCPossible[T: Manifest](pc: Element): Boolean =
        _toCCPossible(pc) match {
            case Some(_) => true
            case _ => false
        }

    /**
     * deserializes a given case class type from a given Node instance
     * throws a IllegalArgumentException if a Nodes properties
     * do not fit to the case class properties
     */
    def deSerialize[T: Manifest](pc: Element): T = {
        toCC[T](pc) match {
            case Some(t) => t
            case _ => throw new IllegalArgumentException("given Case Class: " +
                manifest[T].erasure.getName + " does not fit to serialized properties")
        }
    }
}

