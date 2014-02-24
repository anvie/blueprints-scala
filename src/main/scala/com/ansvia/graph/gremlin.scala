package com.ansvia.graph

import com.tinkerpop.pipes.PipeFunction
import com.tinkerpop.pipes.util.structures.{Pair => BPPair}
import com.tinkerpop.blueprints.Vertex

package object gremlin {

    import scala.language.implicitConversions

    implicit def tupleToPair[A,B](pair:(A, B)) = new BPPair[A,B](pair._1, pair._2)

    implicit def gremlinPipeFuncWrapper[A,B](func:(A) => B) = {
        new PipeFunction[A,B]{
            def compute(v:A):B = {
                func.apply(v)
            }
        }
    }

    implicit def gremlinPipeFilterFuncWrapper(func:(Vertex) => Boolean) = {
        new PipeFunction[Vertex,java.lang.Boolean]{
            def compute(v:Vertex):java.lang.Boolean = {
                func.apply(v)
            }
        }
    }

    implicit def gremlinPipeOrderFuncWrapper(func:(Vertex, Vertex) => Int) = {
        new PipeFunction[BPPair[Vertex, Vertex], java.lang.Integer] {
            def compute(arg: BPPair[Vertex, Vertex]):java.lang.Integer =
                func.apply(arg.getA, arg.getB)
        }
    }
}

