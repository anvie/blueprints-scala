package com.ansvia.graph.util

import scalax.rules.scalasig._
import collection.mutable.ArrayBuffer
import java.lang.reflect
import com.ansvia.graph.BlueprintsWrapper.DbObject
import collection.mutable
import com.ansvia.graph.annotation.Persistent
import annotation.tailrec
import scala.reflect.ClassTag

/**
 * helper class to store Class object
 */
case class JavaType(c: Class[_])

/**
 * Case Class deserializing object
 */
object CaseClassDeserializer {

    /**
     * Method Map cache for method serialize
     */
    private val methodCache = new mutable.HashMap[Class[_], Map[String, java.lang.reflect.Method]]()
        with mutable.SynchronizedMap[Class[_], Map[String, java.lang.reflect.Method]]

    private val methodSetterCache = new mutable.HashMap[Class[_], Map[String, java.lang.reflect.Method]]()
        with mutable.SynchronizedMap[Class[_], Map[String, java.lang.reflect.Method]]

//    private val persistedVarCache = new mutable.HashMap[Class[_], Array[String]]()
//        with mutable.SynchronizedMap[Class[_], Array[String]]

    /**
     * signature parser cache
     */
    private val sigParserCache = new mutable.HashMap[Class[_], Seq[(String, JavaType)]]()
        with mutable.SynchronizedMap[Class[_], Seq[(String, JavaType)]]

    /**
     * default behaviour for T == serialized class
     */
    def deserialize[T](m: Map[String, AnyRef])(implicit tag: ClassTag[T]): T =
        deserialize[T](tag.runtimeClass, m)

    /**
     * convenience method using class manifest
     * use it like <code>val test = deserialize[Test](myMap)<code>
     */
    def deserialize[T](serializedClass: Class[_], m: Map[String, AnyRef])(implicit tag: ClassTag[T]): T =
        deserialize(m, JavaType(serializedClass)).asInstanceOf[T]

    /**
     * Creates a case class instance from parameter map
     *
     * @param m Map[String, AnyRef] map of parameter name an parameter type
     * @param javaTypeTarget JavaType case class class to create
     */
    def deserialize(m: Map[String, AnyRef], javaTypeTarget: JavaType) = {
        require(javaTypeTarget.c.getConstructors.length == 1, "Case classes must only have one constructor.")

        val constructor = javaTypeTarget.c.getConstructors.head
        val params = sigParserCache.getOrElseUpdate(javaTypeTarget.c, CaseClassSigParser.parse(javaTypeTarget.c))

        val values = new ArrayBuffer[AnyRef]
        for ((paramName, paramType) <- params) {
            val field = m.getOrElse(paramName, null)

            field match {
                // use null if the property does not exist
                case null =>
                    values += null
                // if the value is directly assignable: use it
                case x: AnyRef if (x.getClass.isAssignableFrom(paramType.c)) =>
                    values += x
                case x: Array[_] =>
                    values += x
                // otherwise try to create an instance using der String Constructor
                case x: AnyRef =>
                    val paramCtor = paramType.c.getConstructor(classOf[String])
                    val value = paramCtor.newInstance(x).asInstanceOf[AnyRef]
                    values += value
            }
        }

        val paramsCount = constructor.getParameterTypes.length
        val ccParams = values.slice(0, paramsCount)

        val summoned = constructor.newInstance(ccParams.toArray: _*).asInstanceOf[AnyRef]

        val methods = {

            var symbols = Map.empty[String, reflect.Method]
            var curClazz:Class[_] = javaTypeTarget.c
            var done = false

            while(!done){

                val rv: Map[String, reflect.Method] =
                    methodSetterCache.getOrElseUpdate(curClazz,
                        curClazz.getDeclaredMethods
                         .filter{ z =>
                            z.getParameterTypes.length == 1
                        }.map {
                            m => m.getName -> m
                        }.toMap)

                symbols ++= rv

                curClazz = curClazz.getSuperclass
                done = curClazz == classOf[java.lang.Object] || curClazz == null
            }

            symbols
        }


        for ((paramName, paramType) <- params){
            val field = m.getOrElse(paramName, null)

            // scala using _$eq suffix for setter method name
            val paramNameSet = paramName + "_$eq"

            field match {
                // use null if the property does not exist
                case null =>
                    // skip null
                // if the value is directly assignable: use it
                case x: AnyRef if (x.getClass.isAssignableFrom(paramType.c)) =>
                    methods.get(paramNameSet).map(_.invoke(summoned, x))
                case x: Array[_] =>
                    methods.get(paramNameSet).map(_.invoke(summoned, x))
                // otherwise try to create an instance using der String Constructor
                case x: AnyRef =>
                    val paramCtor = paramType.c.getConstructor(classOf[String])
                    val value = paramCtor.newInstance(x).asInstanceOf[AnyRef]
                    methods.get(paramNameSet).map(_.invoke(summoned, value))
            }
        }

        summoned
    }

    /**
     * creates a map from case class parameter
     * @param o AnyRef case class instance
     */
    def serialize(o: AnyRef): Map[String, AnyRef] = {

        val methods = {

            var symbols = Map.empty[String, reflect.Method]
            var curClazz:Class[_] = o.getClass
            var done = false

            while(!done){

                val rv: Map[String, reflect.Method] =
                    methodCache.getOrElseUpdate(curClazz,
                        curClazz.getDeclaredMethods
                            .filter{ z =>
                                z.getParameterTypes.isEmpty
                            }.map {
                                m => m.getName -> m
                            }.toMap)

                symbols ++= rv

                curClazz = curClazz.getSuperclass
                done = curClazz == classOf[java.lang.Object] || curClazz == null
            }

            symbols
        }

        val params: Seq[(String, JavaType)] = sigParserCache.getOrElseUpdate(o.getClass, CaseClassSigParser.parse(o.getClass))
        val l = for {
            (paramName, jt) <- params;
            value = methods.get(paramName).get.invoke(o)
        } yield {
            (paramName, value)
        }
        l.toMap
    }

    def getParsedParams(k:Class[_]):Option[Seq[(String, JavaType)]] = {
        Some(sigParserCache.getOrElseUpdate(k.getClass, CaseClassSigParser.parse(k.getClass)))
    }
}

class MissingPickledSig(clazz: Class[_]) extends Error("Failed to parse pickled Scala signature from: %s".format(clazz))

class MissingExpectedType(clazz: Class[_]) extends Error(
    "Parsed pickled Scala signature, but no expected type found: %s"
        .format(clazz)
)

object CaseClassSigParser {
    val SCALA_SIG = "ScalaSig"
    val SCALA_SIG_ANNOTATION = "Lscala/reflect/ScalaSignature;"
    val BYTES_VALUE = "bytes"

    protected def parseScalaSig[A](clazz: Class[A]): Option[ScalaSig] = {
        val firstPass = ScalaSigParser.parse(clazz)
        firstPass match {
            case Some(x) => {
                Some(x)
            }
            case None if clazz.getName.endsWith("$") => {
                val clayy = Class.forName(clazz.getName.replaceFirst("\\$$", ""))
                val secondPass = ScalaSigParser.parse(clayy)
                secondPass
            }
            case x => x
        }
    }

    protected def findSym[A](clazz: Class[A]): SymbolInfoSymbol with Product with Serializable = {
        val pss = parseScalaSig(clazz)
        pss match {
            case Some(x) => {
                val topLevelClasses = x.topLevelClasses
                topLevelClasses.headOption match {
                    case Some(tlc) => {
                        tlc
                    }
                    case None => {
                        val topLevelObjects = x.topLevelObjects
                        topLevelObjects.headOption match {
                            case Some(tlo) => {
                                tlo
                            }
                            case _ => throw new MissingExpectedType(clazz)
                        }
                    }
                }
            }
            case None => throw new MissingPickledSig(clazz)
        }
    }

    private val persistedVarCache = new mutable.HashMap[Class[_], Array[String]]()
        with mutable.SynchronizedMap[Class[_], Array[String]]
//    private val traitItCache = new mutable.HashMap[Class[_], Seq[Class[_]]]()
//        with mutable.SynchronizedMap[Class[_], Seq[Class[_]]]

    private val classesTreeCache = new mutable.HashMap[Class[_], Array[Class[_]]]()
        with mutable.SynchronizedMap[Class[_], Array[Class[_]]]

    private def isExcluded(clazz: Class[_]) = {
        clazz == classOf[java.lang.Object] ||
            clazz == classOf[scala.ScalaObject] ||
            clazz == classOf[scala.Product] ||
            clazz == classOf[scala.Serializable] ||
            clazz == classOf[DbObject] ||
            clazz == null
    }

//    @tailrec
    private def crawlClassesTree(clazz:Class[_]):Array[Class[_]] = {

        classesTreeCache.getOrElseUpdate(clazz,
            {
                var rv:Array[Class[_]] = clazz.getInterfaces.flatMap { c =>
                    if (c!=null && !isExcluded(c))
                        crawlClassesTree(c) ++ Array(c)
                    else
                        Array.empty[Class[_]]
                }

                val sp = clazz.getSuperclass
                if (!isExcluded(sp)){
                    rv ++= crawlClassesTree(sp)
                }

                rv
            }
        )


    }


    def parse[A](clazz: Class[A]): Seq[(String, JavaType)] = {

        var symbols = Array.empty[(String, JavaType)]
        var curClazz:Class[_] = clazz
        var done = false
        var traitIterator = curClazz.getInterfaces.toIterator
        val mainClazz = clazz

        // fill cache
        if (persistedVarCache.get(mainClazz).isEmpty){

            var fieldNames = Array.empty[String]

            while(!done){
                fieldNames ++= curClazz.getDeclaredFields.filter { v =>
                    v.isAnnotationPresent(classOf[Persistent])
                }.map(_.getName)

                curClazz = curClazz.getSuperclass
                done = isExcluded(curClazz)

                if (done && traitIterator.hasNext){

                    // try searching interfaces / traits
                    curClazz = traitIterator.next()

                    done = isExcluded(curClazz)
                }

            }
            persistedVarCache.update(mainClazz, fieldNames)
        }

        curClazz = mainClazz
        traitIterator = crawlClassesTree(curClazz).toIterator
        done = false

        while(!done){

//            println("curClazz: " + curClazz.getSimpleName)

            val rv =
                findSym(curClazz).children
                .filter{ c =>
                    if (c.isCaseAccessor && !c.isPrivate){
                        true
                    }else if (c.isAccessor && !c.isPrivate && !c.isLazy && !c.isProtected){

                        val pv = persistedVarCache.get(mainClazz).get

//                        if (pv.length > 0)
//                            println(curClazz.getSimpleName + ": " + pv.reduceOption(_ + ", " + _).getOrElse("") + " contains " + c.name + "?")

                        pv.contains(c.name)

                    }else{
                        false
                    }
                }.map(_.asInstanceOf[MethodSymbol])
                .zipWithIndex
                .flatMap {
                    case (ms, idx) => {
                        ms.infoType match {
                            case NullaryMethodType(t: TypeRefType) =>
                                Some(ms.name -> typeRef2JavaType(t))
                            case _ =>
                                None
                        }
                    }
                }
            symbols ++= rv

            curClazz = curClazz.getSuperclass
            done = curClazz == classOf[java.lang.Object] || curClazz == null

            if (done && traitIterator.hasNext){

                // try searching interfaces / traits
                curClazz = traitIterator.next()

                done = isExcluded(curClazz)
            }


        }
        symbols.toSeq
    }

    protected def typeRef2JavaType(ref: TypeRefType): JavaType = {
        try {
            JavaType(loadClass(ref.symbol.path))
        } catch {
            case e: Throwable => {
                e.printStackTrace()
                null
            }
        }
    }

    protected def loadClass(path: String) = path match {
        case "scala.Predef.Map" => classOf[Map[_, _]]
        case "scala.Predef.Set" => classOf[Set[_]]
        case "scala.Predef.String" => classOf[String]
        case "scala.package.List" => classOf[List[_]]
        case "scala.package.Seq" => classOf[Seq[_]]
        case "scala.package.Sequence" => classOf[Seq[_]]
        case "scala.package.Collection" => classOf[Seq[_]]
        case "scala.package.IndexedSeq" => classOf[IndexedSeq[_]]
        case "scala.package.RandomAccessSeq" => classOf[IndexedSeq[_]]
        case "scala.package.Iterable" => classOf[Iterable[_]]
        case "scala.package.Iterator" => classOf[Iterator[_]]
        case "scala.package.Vector" => classOf[Vector[_]]
        case "scala.package.BigDecimal" => classOf[BigDecimal]
        case "scala.package.BigInt" => classOf[BigInt]
        case "scala.package.Integer" => classOf[java.lang.Integer]
        case "scala.package.Character" => classOf[java.lang.Character]
        case "scala.Long" => classOf[java.lang.Long]
        case "scala.Int" => classOf[java.lang.Integer]
        case "scala.Boolean" => classOf[java.lang.Boolean]
        case "scala.Short" => classOf[java.lang.Short]
        case "scala.Byte" => classOf[java.lang.Byte]
        case "scala.Float" => classOf[java.lang.Float]
        case "scala.Double" => classOf[java.lang.Double]
        case "scala.Char" => classOf[java.lang.Character]
        case "scala.Any" => classOf[Any]
        case "scala.AnyRef" => classOf[AnyRef]
        case name => Class.forName(name)
    }
}

