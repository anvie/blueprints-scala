
package com.ansvia.graph

import com.tinkerpop.blueprints._
import java.lang.Iterable
import java.util
import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle
import com.tinkerpop.gremlin.java.GremlinPipeline
import scala.Some

object BlueprintsWrapper {
    import scala.collection.JavaConverters._
    import scala.collection.JavaConversions._

    sealed trait Wrapper

    trait ScalasticPropertyAccessor[A <: Element] {
        implicit var o:A

        /**
         * Syntactic sugar property getter.
         * will return Option[T].
         * example usage:
         *
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
         *
         * @param key property key.
         * @tparam T template to return.
         * @return
         */
        def get[T](key:String):Option[T] = {
            o.getProperty(key) match {
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
         * val x = vertex.getOrElse[String]("phone", "-")
         *
         * @param key property key.
         * @param default default value when empty.
         * @tparam T return type.
         * @return
         */
        def getOrElse[T](key:String, default:T):T = {
            o.getProperty(key) match {
                case v:T => v
                case x => {
                    if (x != null){
                        println("x: " + x.getClass.getName)
                    }
                    default
                }
            }
        }

        /**
         * Syntactic sugar for property setter.
         * @param key property key.
         * @param value property value.
         */
        def set(key:String, value:Any){
            o.setProperty(key, value)
        }
    }

    /**
     * Edge wrapper on arrow chain.
     * This wrapper automatic used via ToVertexWrapper.
     * @param vertex vertex
     * @param label label
     * @param db database
     */
    case class ToEdgeWrapper(var vertex:Vertex, var label:String, db:Graph) extends Wrapper {
        private var lastEdge:Edge = null
        var prev:Option[ToVertexWrapper] = None

        def -->(inV:Vertex) = {
            lastEdge = db.addEdge(null, vertex, inV, label)

            // for performance reason
            // we using previous object if any

            val p = prev.getOrElse {
                ToVertexWrapper(inV, label, db)
            }

            p.prev = Some(this)
            p.o = inV
            p
        }

        def <--(outV:Vertex) = {
            lastEdge = db.addEdge(null, outV, vertex, label)

            // for performance reason
            // we using previous object if any

            val p = prev.getOrElse {
                ToVertexWrapper(outV, label, db)
            }
            p.prev = Some(this)
            p.o = outV
            p
        }

        def < = this.lastEdge
    }

    /**
     * Vertex wrapper on arrow chain.
     * This wrapper automatically called via implicit vertexWrapper function.
     * @param vertex vertex.
     * @param label label.
     * @param db database object.
     */
    case class ToVertexWrapper(private val vertex:Vertex, var label:String, db:Graph)
            extends Wrapper with ScalasticPropertyAccessor[Vertex] {

        implicit var o = vertex
        var prev:Option[ToEdgeWrapper] = None

        def -->(label:String):ToEdgeWrapper = {
            this.label = label

            // for performance reason
            // we using previous object if any

            val next = prev.getOrElse {
                ToEdgeWrapper(o, label, db)
            }
            next.prev = Some(this)
            next.vertex = o
            next.label = label
            next
        }

        def <--(label:String):ToEdgeWrapper = {
            this.label = label

            // for performance reason
            // we using previous object if any

            val next = prev.getOrElse {
                ToEdgeWrapper(o, label, db)
            }
            next.prev = Some(this)
            next.vertex = o
            next.label = label
            next
        }

        def < = {
            if (this.prev.isDefined)
                this.prev.get <
            else
                null
        }

        /**
         * Create mutual connection.
         * @param label edge label.
         * @return
         */
        def <-->(label:String):ToVertexWrapper = {
            this.label = label
            this
        }

        /**
         * Create mutual connection.
         * @param bothV another vertex to connect.
         * @return
         */
        def <-->(bothV:Vertex):ToVertexWrapper = {
            assert(label != null, "no label?")
            db.addEdge(null, o, bothV, label)
            db.addEdge(null, bothV, o, label)
            // update current vertex in chain
            o = bothV
            this
        }

        /**
         * Get list of mutual vertices.
         * @param label label name.
         * @return
         */
        def mutual(label:String):Iterable[Vertex] = {
            val vx = this.pipe.both(label).toList
            vx.filter { v =>
                v.getId != o.getId &&
                vx.count( vv => v == vv ) == 2
            }
        }

        /**
         * get gremlin pipe from the vertex.
         * @return
         */
        def pipe = {
            val pipe = new GremlinPipeline[Vertex, AnyRef]()
            pipe.start(o)
        }

    }


    case class EdgeWrapperRight(v1:Vertex, edge:Edge, label:String, db:Graph) extends Wrapper with ScalasticPropertyAccessor[Edge] {
        implicit var o = edge

        def -->(v2:Vertex) = {
            db.addEdge(null, v1, v2, label)
        }
    }

    case class EdgeWrapperLeft(edge:Edge, db:Graph) extends Wrapper with ScalasticPropertyAccessor[Edge] {
        implicit var o = edge

        def -->(label:String):EdgeWrapperRight = {
            val v = edge.getVertex(Direction.OUT)
            EdgeWrapperRight(v, o, label, db)
        }

        def <--(label:String):ToVertexWrapper = {
            val v = edge.getVertex(Direction.IN)
            ToVertexWrapper(v, label, db)
        }
    }

    implicit def vertexWrapper(vertex:Vertex)(implicit db:Graph) = ToVertexWrapper(vertex, null, db)
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
}
