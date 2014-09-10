package com.ansvia.graph

import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.util.structures.{Pair => BPPair}
import com.tinkerpop.blueprints.{Edge, Element, Vertex}

package object gremlin {

//    import scala.language.implicitConversions

    implicit def tupleToPair[A,B](pair:(A, B)) = new BPPair[A,B](pair._1, pair._2)

    implicit def gremlinPipeFuncWrapper[A,B](func:(A) => B) = {
        new PipeFunction[A,B]{
            def compute(v:A):B = {
                func.apply(v)
            }
        }
    }

    private def gremlinPipeFilterFuncWrapperT[T <: Element](func:(T) => Boolean) = {
        new PipeFunction[T,java.lang.Boolean]{
            def compute(elm:T):java.lang.Boolean = {
                func.apply(elm)
            }
        }
    }

    implicit def gremlinPipeFilterFuncWrapperVertex = gremlinPipeFilterFuncWrapperT[Vertex] _
    implicit def gremlinPipeFilterFuncWrapperEdge = gremlinPipeFilterFuncWrapperT[Edge] _

    implicit def gremlinPipeOrderFuncWrapper[T <: Element](func:(T, T) => Int) = {
        new PipeFunction[BPPair[T, T], java.lang.Integer] {
            def compute(arg: BPPair[T, T]):java.lang.Integer =
                func.apply(arg.getA, arg.getB)
        }
    }

}

