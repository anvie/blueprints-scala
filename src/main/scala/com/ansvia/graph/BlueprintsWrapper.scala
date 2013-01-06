
package com.ansvia.graph

import com.tinkerpop.blueprints._
import java.lang.Iterable
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.gremlin.java.GremlinPipeline
import scala.Some
import com.tinkerpop.pipes.util.FastNoSuchElementException
import com.ansvia.graph.Exc.NotBoundException

object BlueprintsWrapper {
    import scala.collection.JavaConversions._

    sealed trait Wrapper

    case class ScalasticPropertyAccessor[A <: Element](obj:A) {

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
        def get[T](key:String):Option[T] = {
            obj.getProperty(key) match {
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
        def getOrElse[T](key:String, default:T):T = {
            obj.getProperty(key) match {
                case v:T => v
                case x => {
//                    if (x != null){
//                        println("x: " + x.getClass.getName)
//                    }
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
         * Deserialize object to case class.
         * @tparam T case class type.
         * @return
         */
        def toCC[T : Manifest]:Option[T] = {
            ObjectConverter.toCC[T](obj)
        }
    }

    implicit def elmToPropertyAccessor(elm:Element) = ScalasticPropertyAccessor(elm)

//
//    case class DbObjectSaver(elm:DbObject, db:Graph){
//        /**
//         * Save this object to database.
//         */
//        def save() = {
//            db.save(elm)
//        }
//    }
//
//    implicit def elmToObjectSaver(elm:DbObject)(implicit db:Graph) = DbObjectSaver(elm, db)

    /**
     * Edge wrapper on arrow chain.
     * This wrapper automatic used via VertexWrapper.
     * @param vertex vertex
     * @param label label
     * @param db database
     */
    case class EdgeWrapper(var vertex:Vertex, var label:String, db:Graph) extends Wrapper {
        private var lastEdge:Edge = null
        var prev:Option[VertexWrapper] = None

        def -->(inV:Vertex):VertexWrapper = {
            lastEdge = db.addEdge(null, vertex, inV, label)

            // for performance reason
            // we using previous object if any

            val p = prev.getOrElse {
                VertexWrapper(inV, label, db)
            }

            p.prev = Some(this)
            p.vertex = inV
            p
        }

        def <--(outV:Vertex):VertexWrapper = {
            lastEdge = db.addEdge(null, outV, vertex, label)

            // for performance reason
            // we using previous object if any

            val p = prev.getOrElse {
                VertexWrapper(outV, label, db)
            }
            p.prev = Some(this)
            p.vertex = outV
            p
        }

        def -->(o:DbObject):VertexWrapper = {
            this.-->(o.getVertex)
        }

        def <():Edge = this.lastEdge
    }

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
            val pipe = new GremlinPipeline[Vertex, AnyRef]()
            pipe.start(vertex)
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


    /**
     * Gremlin pipe wrapper.
     * @param innerPipe raw gremlin pipe.
     */
    case class GremlinPipeWrapperVertex(innerPipe:GremlinPipeline[Vertex, Vertex]){
        def wrap = GremlinPipeWrapperVertex(innerPipe)

        def filter(gpf: Vertex => Boolean):GremlinPipeline[Vertex, Vertex] = {
            val rv = innerPipe.filter(new PipeFunction[Vertex,java.lang.Boolean] {
                def compute(v: Vertex):java.lang.Boolean = {
                    gpf.apply(v)
                }
            })
            rv
        }

        /**
         * Get first in direction for label vertex.
         * @param label edge label.
         * @return
         */
        def inFirst(label:String):Option[Vertex] = {
            try {
                Some(innerPipe.in(label).next())
            }catch{
                case e:FastNoSuchElementException => None
            }
        }

        /**
         * Get first head out direction for label vertex.
         * @param label edge label.
         * @return
         */
        def outFirst(label:String):Option[Vertex] = {
            try {
                Some(innerPipe.out(label).next())
            }catch{
                case e:FastNoSuchElementException => None
            }
        }
    }

    case class GremlinPipeWrapperEdge[Vertex, Edge](innerPipe:GremlinPipeline[Vertex, Edge]){
        def wrap = GremlinPipeWrapperEdge[Vertex, Edge](innerPipe)
        def filter(gpf: Edge => Boolean):GremlinPipeline[Vertex, Edge] = {
            val rv = innerPipe.filter(new PipeFunction[Edge,java.lang.Boolean] {
                def compute(e: Edge):java.lang.Boolean = {
                    gpf.apply(e)
                }
            })
            rv
        }
    }
    implicit def gremlinPipeWrapperVertex(pipe:GremlinPipeline[Vertex, Vertex]) = GremlinPipeWrapperVertex(pipe)
    implicit def gremlinPipeWrapperEdge(pipe:GremlinPipeline[Vertex, Edge]) = GremlinPipeWrapperEdge(pipe)

    /**
     * Working in transactional fashion.
     * @param wrappedFunc function
     * @param db implicit db
     * @return
     */
    def transact[T](wrappedFunc: => T)(implicit db:TransactionalGraph):T = {
//        val dbx = db.startTransaction()
        try {

            val x = wrappedFunc

            db.stopTransaction(TransactionalGraph.Conclusion.SUCCESS)

            x

        }catch{
            case e:Exception =>
                db.stopTransaction(TransactionalGraph.Conclusion.FAILURE)
                throw e
        }
    }

    implicit def dbWrapper(db:Graph) = new {
        def save[T:Manifest](cc:T):Vertex = {
            val o = db.addVertex(null)
            ObjectConverter.serialize(cc.asInstanceOf[AnyRef], o)
        }
    }

    trait DbObject {

        private var vertex:Vertex = null

        /**
         * Save this object to database.
         */
        def save()(implicit db:Graph) = {
            db.save(this)
        }

        /**
         * override this for custom load routine
         * @param vertex vertex object.
         */
        def __load__(vertex:Vertex){
           this.vertex = vertex
        }

        /**
         * get bounded vertex.
         * throw [[com.ansvia.graph.Exc.NotBoundException]] if object not saved
         * see [[com.ansvia.graph.BlueprintsWrapper.DbObject#isSaved]] for checking is object saved or not.
         * @return
         */
        def getVertex = {
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
        def -->(label:String)(implicit db:Graph) = {
            vertex --> label
        }

    }
}

