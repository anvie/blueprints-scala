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
import reflect.ClassTag

object ObjectConverter {

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
            case (name, value) =>
                // set only if different with current (eg: new changes)
                if (pc.getProperty(name) != value)
                    pc.setProperty(name, value)
                //else
                //    println("ignored (not different): " + name + " <-> " + value)
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
    def toCC[T: ClassTag](pc: Element): Option[T] =
        _toCCPossible(pc) match {
            case Some(serializedClass) =>

                val kv = for (k <- pc.getPropertyKeys; v = pc.getProperty[AnyRef](k)) yield (k -> v)

                val o = CaseClassDeserializer.deserialize[T](serializedClass, kv.toMap)

                if (o.isInstanceOf[DbObject]){
                    o.asInstanceOf[DbObject].__load__(pc.asInstanceOf[Vertex])
                }

                Some(o)

            case _ => None
        }

    private def _toCCPossible[T](pc: Element)(implicit tag: ClassTag[T]): Option[Class[_]] = {

        val pv = pc.getProperty[String](CLASS_PROPERTY_NAME)
        if( pv != null ){
            val cpn = pv.toString
            val c = Class.forName(cpn)
            if (tag.runtimeClass.isAssignableFrom(c))
                Some(c)
            else
                None
        } else
            None


    }

    /**
     * only checks if this property container has been serialized
     * with T
     */
    def toCCPossible[T: ClassTag](pc: Element): Boolean =
        _toCCPossible(pc) match {
            case Some(_) => true
            case _ => false
        }

    /**
     * deserializes a given case class type from a given Node instance
     * throws a IllegalArgumentException if a Nodes properties
     * do not fit to the case class properties
     */
    def deSerialize[T](pc: Element)(implicit tag: ClassTag[T]): T = {
        toCC[T](pc) match {
            case Some(t) => t
            case _ => throw new IllegalArgumentException("given Case Class: " +
                tag.runtimeClass.getName + " does not fit to serialized properties")
        }
    }
}
