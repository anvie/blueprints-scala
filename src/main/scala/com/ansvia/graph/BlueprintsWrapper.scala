package com.ansvia.graph

import com.tinkerpop.blueprints._
import java.lang.Iterable
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.gremlin.java.GremlinPipeline
import com.tinkerpop.pipes.util.FastNoSuchElementException
import com.ansvia.graph.Exc.NotBoundException
import com.tinkerpop.pipes.util.structures.{Pair => BPPair}
import sun.reflect.Reflection
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import com.tinkerpop.blueprints.util.wrappers.id.{IdVertex, IdGraph}



object BlueprintsWrapper {
    import scala.collection.JavaConversions._

  val defaultClassloader = if(Reflection.getCallerClass!=null) Reflection.getCallerClass.getClassLoader else null


    case class ScalasticPropertyAccessor[A <: Element : ClassTag](var obj:A) {

        /**
         * Syntactic sugar property getter.
         * will return Option[T].
         * example usage:
         *
         * <pre>
         * val x = vertex.get[String]("location")
         * if (x.isDefined){
         *      // do here if defined
         * }
         *
         * or using map
         *
         * vertex.get[String]("location") map { location =>
         *      // do with naked location (String)
         * }
         * </pre>
         *
         * @param key property key.
         * @tparam T template to return.
         * @return
         */
        def get[T](key:String)(implicit tag: TypeTag[T]):Option[T] = {
            obj.getProperty[AnyRef](key) match {
                case v:T => Some(v)
                case x => None
            }
        }

        /**
         * Syntactic sugar property getter with
         * default value if not defined.
         *
         * Example:
         *
         * <pre>
         * val x = vertex.getOrElse[String]("phone", "-")
         * </pre>
         *
         * @param key property key.
         * @param default default value when empty.
         * @tparam T return type.
         * @return
         */
        def getOrElse[T](key:String, default:T)(implicit tag: TypeTag[T]):T = {
            obj.getProperty[AnyRef](key) match {
                case v:T => v
                case x => {
                    default
                }
            }
        }

        /**
         * Syntactic sugar for property setter.
         * @param key property key.
         * @param value property value.
         */
        def set(key:String, value:Any) = {
            obj.setProperty(key, value)
            this
        }

        /**
         * Check is has key
         * @param key property key
         * @return
         */
        def has(key:String):Boolean = {
            obj.getProperty(key) != null
        }

        /**
         * Reload
         * @param db
         * @return
         */
        def reload()(implicit db:Graph) = {
            _reloadInner(db)
        }

        private def _reloadInner(db:Graph)(implicit tag:ClassTag[A]) = {
            tag.runtimeClass.toString match {
                case "interface com.tinkerpop.blueprints.Vertex" =>
                    db.getVertex(obj.asInstanceOf[Vertex].getId).asInstanceOf[A]
                case "interface com.tinkerpop.blueprints.Edge" =>
                    db.getEdge(obj.asInstanceOf[Edge].getId).asInstanceOf[A]
            }
        }

        /**
         * Deserialize object to case class.
         * @tparam T case class type.
         */
        def toCC[T: ClassTag]:Option[T] = toCC[T](defaultClassloader)

      /**
       * Deserialize object to case class.
       * @tparam T case class type.
       * @param classLoader explicitly specified classloader if needed.
       */
      def toCC[T: ClassTag](classLoader: ClassLoader):Option[T] = {
        ObjectConverter.toCC[T](obj, classLoader)
      }
    }

    implicit def vertexToPropertyAccessor(elm:Vertex) = ScalasticPropertyAccessor(elm)
    implicit def edgeToPropertyAccessor(elm:Edge) = ScalasticPropertyAccessor(elm)
    implicit def elementToPropertyAccessor(elm:Element) = ScalasticPropertyAccessor(elm)


    /**
     * Vertex wrapper on arrow chain.
     * This wrapper automatically called via implicit vertexWrapper function.
     * @param vertex vertex.
     * @param label label.
     * @param db database object.
     */
    case class VertexWrapper(var vertex:Vertex, var label:String, db:Graph)
            extends Wrapper {

        var prev:Option[EdgeWrapper] = None

        def -->(label:String):EdgeWrapper = {
            this.label = label

            // for performance reason
            // we using previous object if any

            val next = prev.getOrElse {
                EdgeWrapper(vertex, label, db)
            }
            next.prev = Some(this)
            next.vertex = vertex
            next.label = label
            next
        }

        def <--(label:String):EdgeWrapper = {
            this.label = label

            // for performance reason
            // we using previous object if any

            val next = prev.getOrElse {
                EdgeWrapper(vertex, label, db)
            }
            next.prev = Some(this)
            next.vertex = vertex
            next.label = label
            next
        }

        def <():Edge = {
            if (this.prev.isDefined)
                this.prev.get <()
            else
                null
        }

        /**
         * Create mutual connection.
         * @param label edge label.
         * @return
         */
        def <-->(label:String):VertexWrapper = {
            this.label = label
            this
        }

        /**
         * Create mutual connection.
         * @param bothV another vertex to connect.
         * @return
         */
        def <-->(bothV:Vertex):VertexWrapper = {
            assert(label != null, "no label?")
            db.addEdge(null, vertex, bothV, label)
            db.addEdge(null, bothV, vertex, label)
            // update current vertex in chain
            vertex = bothV
            this
        }

        /**
         * Get list of mutual vertices.
         * @param label label name.
         * @return
         */
        def mutual(label:String):Iterable[Vertex] = {
            val vx = vertex.getVertices(Direction.BOTH, label).toList
            vx.filter { v =>
                v.getId != vertex.getId &&
                vx.count( vv => v == vv ) == 2
            }
        }

        /**
         * get gremlin pipe from the vertex.
         * @return
         */
        def pipe = {
            (new GremlinPipeline[Vertex, AnyRef]()).start(vertex)
        }

    }


    case class EdgeWrapperRight(vertex:Vertex, edge:Edge, label:String, db:Graph) extends Wrapper {
        def -->(v2:Vertex) = {
            db.addEdge(null, vertex, v2, label)
        }
    }

    case class EdgeWrapperLeft(edge:Edge, db:Graph) extends Wrapper {
        def -->(label:String):EdgeWrapperRight = {
            val v = edge.getVertex(Direction.OUT)
            EdgeWrapperRight(v, edge, label, db)
        }

        def <--(label:String):VertexWrapper = {
            val vertex = edge.getVertex(Direction.IN)
            VertexWrapper(vertex, label, db)
        }
    }

    implicit def vertexWrapper(vertex:Vertex)(implicit db:Graph) = VertexWrapper(vertex, null, db)
    implicit def edgeWrapper(edge:Edge)(implicit db:Graph) = EdgeWrapperLeft(edge, db)
    implicit def edgeFormatter(edge:Edge) = new {
        def prettyPrint(key:String) = {
            val in = edge.getVertex(Direction.IN)
            val label = edge.getLabel
            val out = edge.getVertex(Direction.OUT)
            "%s -->%s--> %s".format(out.getProperty(key), label, in.getProperty(key))
        }
    }
    implicit def edgeIterableDumper(edges:Iterable[Edge]) = new {
        def printDump(key:String){
            edges.foreach( edge => println(edge.prettyPrint(key)) )
        }
    }
    implicit def vertexIterableDumper(vx:Iterable[Vertex]) = new {
        def printDump(title:String, key:String)(implicit db:Graph){
            println(title)
            vx.foreach( v => println(" + " + v.getOrElse[String](key, "id:" + v.getId.toString)) )
        }
        def printDumpGetList(title:String, key:String)(implicit db:Graph) = {
            println(title)
            val vxList = vx.toList
            vx.foreach( v => println(" + " + v.getOrElse[String](key, "id:" + v.getId.toString)) )
            vxList
        }
    }

//
//    /**
//     * Gremlin pipe wrapper.
//     * @param innerPipe raw gremlin pipe.
//     */
//    @deprecated()
//    case class GremlinPipeWrapperVertex(innerPipe:GremlinPipeline[Vertex, Vertex]){
//
//        @deprecated()
//        def wrap = GremlinPipeWrapperVertex(innerPipe)
//
////        /**
////         * Filter vertex out.
////         * Example:
////         *
////         * vertex.pipe.out("friend").wrap.filter { v =>
////         *      v.get[String]("name").get != "andrie"
////         * }
////         *
////         * @param gpf
////         * @return
////         */
////        @deprecated()
////        def filter(gpf: Vertex => Boolean):GremlinPipeline[Vertex, Vertex] = {
////            val rv = innerPipe.filter(new PipeFunction[Vertex,java.lang.Boolean] {
////                def compute(v: Vertex):java.lang.Boolean = {
////                    gpf.apply(v)
////                }
////            })
////            rv
////        }
//
//        /**
//         * Order vertices.
//         * Example:
//         *
//         * vertex.pipe.out("friend").wrap.order{ (a,b) =>
//         *      a.getProperty("name").compare(b.getProperty("name"))
//         * }
//         *
//         * @param gpf
//         * @return
//         */
//        @deprecated()
//        def sort(gpf: (Vertex, Vertex) => Int):GremlinPipeline[Vertex, Vertex] = {
//            val rv = innerPipe.order(new PipeFunction[BPPair[Vertex, Vertex], java.lang.Integer] {
//                def compute(argument: BPPair[Vertex, Vertex]):java.lang.Integer = {
//                    gpf.apply(argument.getA, argument.getB)
//                }
//            })
//            rv
//        }
//
//        /**
//         * Get first in direction for label vertex.
//         * @param label edge label.
//         * @return
//         */
//        @deprecated()
//        def inFirst(label:String):Option[Vertex] = {
//            try {
//                Some(innerPipe.in(label).next())
//            }catch{
//                case e:FastNoSuchElementException => None
//            }
//        }
//
//        /**
//         * Get first head out direction for label vertex.
//         * @param label edge label.
//         * @return
//         */
//        @deprecated()
//        def outFirst(label:String):Option[Vertex] = {
//            try {
//                Some(innerPipe.out(label).next())
//            }catch{
//                case e:FastNoSuchElementException => None
//            }
//        }
//    }
//
//    case class GremlinPipeWrapperEdge[Vertex, Edge](innerPipe:GremlinPipeline[Vertex, Edge]){
//        def wrap = GremlinPipeWrapperEdge[Vertex, Edge](innerPipe)
//
//        /**
//         * Filter edge out.
//         * @param gpf
//         * @return
//         */
//        def filter(gpf: Edge => Boolean):GremlinPipeline[Vertex, Edge] = {
//            val rv = innerPipe.filter(new PipeFunction[Edge,java.lang.Boolean] {
//                def compute(e: Edge):java.lang.Boolean = {
//                    gpf.apply(e)
//                }
//            })
//            rv
//        }
//
//        /**
//         * Order edges.
//         * @param gpf
//         * @return
//         */
//        def sort(gpf: (Edge, Edge) => Int):GremlinPipeline[Vertex, Edge] = {
//            val rv = innerPipe.order(new PipeFunction[BPPair[Edge, Edge], java.lang.Integer] {
//                def compute(argument: BPPair[Edge, Edge]):java.lang.Integer = {
//                    gpf.apply(argument.getA, argument.getB)
//                }
//            })
//            rv
//        }
//    }
//    implicit def gremlinPipeWrapperVertex(pipe:GremlinPipeline[Vertex, Vertex]) = GremlinPipeWrapperVertex(pipe)
//    implicit def gremlinPipeWrapperEdge(pipe:GremlinPipeline[Vertex, Edge]) = GremlinPipeWrapperEdge(pipe)

    /**
     * Working in transactional fashion.
     * @param wrappedFunc function
     * @param db implicit db
     * @return
     */
    def transact[T](wrappedFunc: => T)(implicit db:TransactionalGraph):T = {
        try {
            val rv = wrappedFunc
            db.commit()
            rv
        }catch{
            case e:Exception =>
                db.rollback()
                throw e
        }
    }

  //
  //    implicit def dbWrapper(db:Graph) = new {
  //
  //        def save[T:Manifest](cc:T):Vertex = {
  //            val (o, _new) = {
  //                cc match {
  //                    case dbo:DbObject if dbo.isSaved =>
  //                        (db.getVertex(dbo.getVertex.getId), false)
  //                    case dbo:DbObject if !dbo.isSaved =>
  //                        (db.addVertex(null), true)
  //                    case _ =>
  //                        (db.addVertex(null), true)
  //                }
  //            }
  //
  //            val elm:Vertex = ObjectConverter.serialize(cc.asInstanceOf[AnyRef], o, _new)
  //
  //            cc match {
  //                case ccDbo:DbObject =>
  //                    ccDbo.__save__(elm)
  ////                    val kv = ccDbo.__save__()
  ////                    for ( (k, v) <- kv ){
  ////
  ////                        // only set if different/new
  ////                        if(elm.getOrElse(k,null) != v)
  ////                            elm.set(k, v)
  ////
  ////                    }
  //                case _ =>
  //            }
  //            elm
  //        }
  //
  //        def delete[T:Manifest](cc:T):Unit = {
  //            cc match {
  //                case dbo:DbObject if dbo.isSaved =>
  //                    db.removeVertex(dbo.getVertex)
  //                case _ =>
  //            }
  //        }
  //    }
  implicit def dbWrapper(db:Graph) = StdDbWrapper.dbWrapper(db) //new DbWrapper(db)


    /**
     * All model should inherit this trait.
     */
    trait DbObject extends AbstractDbObject {

        protected var vertex:Vertex = null

        /**
         * Save this object to database.
         */
        def save()(implicit db:Graph):Vertex = {
            vertex = db.save(this)
            vertex
        }

        /**
         * Delete this object from database.
         */
        def delete()(implicit db:Graph){
            db.delete(this)
            vertex = null
        }

        /**
         * this method called when loading data from database.
         * override this for custom load routine
         * @param vertex vertex object.
         */
        def __load__(vertex:Vertex){
           this.vertex = vertex
        }

        /**
         * this method called before saving into database,
         * override this for custom kv properties.
         * all return from this method will be saved.
         * by default this is just return empty map.
         * @return Map[String, Any]
         */
        def __save__(vertex:Vertex){}

        private[graph] def setVertex(v:Vertex){
            this.vertex = v
        }

        /**
         * get bounded vertex.
         * throw [[com.ansvia.graph.Exc.NotBoundException]] if object not saved
         * see [[com.ansvia.graph.BlueprintsWrapper.DbObject#isSaved]] for checking is object saved or not.
         * @return
         */
        def getVertex:Vertex = {
            if (vertex == null)
                throw NotBoundException("object %s not bound to existing vertex, unsaved vertex?".format(this))
            vertex
        }

        /**
         * Check is object saved.
         * @return
         */
        def isSaved = vertex != null

        /**
         * Create edge label.
         * @param label edge label.
         * @return
         */
        def -->(label:String)(implicit db:Graph):EdgeWrapper = {
            vertex --> label
        }

        /**
         * Reload object from db.
         * @param db implicit Graph db object.
         * @return this object with updated vertex.
         */
        def reload()(implicit db:Graph):this.type = {
            if (!isSaved)
                throw NotBoundException("object %s not saved yet".format(this))

            val id =
            if (isSaved){
                vertex match {
                    case iv:IdVertex =>

                        db match {
                            case ig:IdGraph[_] =>
                                iv.getId
                            case _ =>
                                iv.getBaseVertex.getId
                        }

                    case _ =>
                        vertex.getId
                }
            }else
                vertex.getId

            val v = db.getVertex(id)

            if (v == null)
                throw NotBoundException("object %s not bound to any vertex".format(this))

            this.vertex = v

            v.toCC[this.type](defaultClassloader).get
        }

    }

    trait IDGetter[IDType] extends AbstractIDGetter[IDType] {
        def isSaved:Boolean
        def getVertex:Vertex

        def getId:IDType = {
            if (!isSaved)
                throw NotBoundException("object %s not saved yet".format(this))
            getVertex.getId.asInstanceOf[IDType]
        }
    }

    trait IdDbObject[IDType] extends DbObject with IDGetter[IDType] {

        type idType = IDType
        protected var id:IDType = _
        private val _nullId:IDType = id

        /**
         * this method called when loading data from database.
         * override this for custom load routine
         * @param vertex vertex object.
         */
        override def __load__(vertex: Vertex) {
            super.__load__(vertex)
            id = vertex.getId.asInstanceOf[IDType]
        }

        def isSaved:Boolean
        def getVertex:Vertex

        override def getId:IDType = {
            if (id != _nullId){
                id
            }else{
                if (!isSaved)
                    throw NotBoundException("object %s not saved yet".format(this))
                id = getVertex.getId.asInstanceOf[IDType]
                id
            }
        }

        /**
         * Save this object to database.
         */
        override def save()(implicit db: Graph): Vertex = {
            val v = super.save()(db)
            id = v.getId.asInstanceOf[IDType]
            v
        }

        private[graph] def setId(id:IDType){
            this.id = id
        }


        /**
         * Reload object from db.
         * @param db implicit Graph db object.
         * @return this object with updated vertex.
         */
        override def reload()(implicit db: Graph) = {
            if (id == _nullId && isSaved){
                vertex match {
                    case iv:IdVertex =>

                        db match {
                            case ig:IdGraph[_] =>
                                id = iv.getId.asInstanceOf[IDType]
                            case _ =>
                                id = iv.getBaseVertex.getId.asInstanceOf[IDType]
                        }

                    case _ =>
                        id = vertex.getId.asInstanceOf[IDType]
                }
            }

            if (id != _nullId){
                vertex = db.getVertex(id)

                if (vertex == null)
                    throw NotBoundException("object %s not bound to any vertex".format(this))

            }else{
                throw NotBoundException("id return null, object %s not saved yet?".format(this))
            }
            vertex.toCC[this.type](defaultClassloader).get
        }
    }

}
