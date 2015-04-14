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
import com.ansvia.graph.util.{CallersContext, CaseClassDeserializer}
import com.ansvia.graph.BlueprintsWrapper.DbObject
import reflect.ClassTag
import scala.collection.mutable
import com.ansvia.graph.Exc.BlueprintsScalaException

object ObjectConverter extends Log {

    /**
     * this name will be used to store the class name of
     * the serialized case class that will be verified
     * in deserialization
     */
    var CLASS_PROPERTY_NAME = "_class_"

    val defaultClassloader = CallersContext.fetchDefaultClassLoader

    /**
     * serializes a given case class into a Node instance
     * for null values not property will be set
     */
    def serialize[T <: Element](cc: AnyRef, pc: Element, newElement:Boolean): T = {
        assert(cc != null, "duno how to serialize null object :(")
        if (newElement){
            val clz = pc.getProperty[String](CLASS_PROPERTY_NAME)
            if (clz != null)
                throw new BlueprintsScalaException("element `" + pc + "` treated as new but already has meta class `" +
                    clz + "` requested to set `" + cc.getClass.getName + "`, we raised this error to prevent data overwrite")
            pc.setProperty(CLASS_PROPERTY_NAME, cc.getClass.getName)
        }

        CaseClassDeserializer.serialize(cc).foreach {
            case (name, null) =>
            case (name, value) =>
                try {
                    assignValue(pc, name, value)
                } catch{
                    case e:IllegalArgumentException =>
                        error("cannot set property %s <= %s\nerror: %s".format(name, value, e.getMessage))
                        throw e
                }

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
    def toCC[T: ClassTag](pc: Element, classLoader: ClassLoader = defaultClassloader): Option[T] =
        _toCCPossible[T](pc, classLoader) match {
            case Some(serializedClass) =>

                var kv:mutable.Set[(String, AnyRef)] = null
                try {
                    kv = for (k <- pc.getPropertyKeys; v = pc.getProperty[AnyRef](k)) yield k -> v

                    val o = CaseClassDeserializer.deserialize[T](serializedClass, kv.toMap)

                    o match {
                      case dbObject: DbObject =>
                        dbObject.__load__(pc.asInstanceOf[Vertex])
                      case _ =>
                    }

                    Some(o)
                }catch{
                    case e:IllegalArgumentException =>
                        error("Cannot deserialize record from db, broken record? \n" +
                            "for class: " + serializedClass.getName + "\n" +
                            {
                                if (kv != null)
                                    "kv: " + kv.toMap + "\n"
                                else
                                    ""
                            } +
                            "error: " + e.getMessage)
                        e.printStackTrace()
                        None
                    case e:IndexOutOfBoundsException =>
                        error("Cannot deserialize record from db, broken record? \n" +
                            "for class: " + serializedClass.getName + "\n" +
                            {
                                if (kv != null)
                                    "kv: " + kv.toMap + "\n"
                                else
                                    ""
                            } +
                            "error: " + e.getMessage)
                        e.printStackTrace()
                        None
                }

            case _ => None
        }

    private def assignValue(pc: Element, attributeName: String, value: Any) {
        value match {
            case Some(x) =>
                assignValue(pc, attributeName, x)
            case None =>
                pc.removeProperty(attributeName)
                ()  // forced Unit
            case _ =>
                if(pc.getProperty(attributeName) != value) {
                    pc.setProperty(attributeName, value)
                }
        }
    }

    private def _toCCPossible[T](pc: Element, classLoader: ClassLoader)(implicit tag: ClassTag[T]): Option[Class[_]] = {
        val pv = pc.getProperty[String](CLASS_PROPERTY_NAME)
        if( pv != null ){
            val cpn = pv.toString
            val c = Class.forName(cpn, true, classLoader)
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
    def toCCPossible[T: ClassTag](pc: Element, classLoader: ClassLoader = defaultClassloader): Boolean =
        _toCCPossible[T](pc, classLoader) match {
            case Some(_) => true
            case _ => false
        }

    /**
     * deserializes a given case class type from a given Node instance
     * throws a IllegalArgumentException if a Nodes properties
     * do not fit to the case class properties
     */
    def deSerialize[T](pc: Element)(implicit tag: ClassTag[T]): T = {
        assert(pc != null, "duno how to deserialize null object :(")
        toCC[T](pc) match {
            case Some(t) => t
            case _ => throw new IllegalArgumentException("given Case Class: " +
                tag.runtimeClass.getName + " does not fit to serialized properties")
        }
    }
}
