package com.ansvia.graph

import com.ansvia.perf.PerfTiming
import org.specs2.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import org.specs2.matcher.MatchResult

import scala.collection.mutable.ListBuffer

/**
 * Author: robin
 * 
 */
class DbObjectComplexSpec extends Specification with PerfTiming {

    import com.ansvia.graph.testing.model._
    import BlueprintsWrapper._

    def is = s2"""$sequential ^
       "Complex DbObject inheritance should
            get level 1 var             ${cdbo.getLevel1Var}
            get level 1b var            ${cdbo.getLevel1bVar}
            get level 2 var             ${cdbo.getLevel2Var}
            get level 2b var            ${cdbo.getLevel2bVar}
            get level 2c var            ${cdbo.getLevel2cVar}
    """

    implicit val db = TinkerGraphFactory.createTinkerGraph()

    val complexDraft = Complex("complex1")
    complexDraft.me = "complex"
    complexDraft.a = 1
    complexDraft.b = 2
    complexDraft.c = 3
    val complex = complexDraft.save().toCC[Complex].get

    def close(){
        db.shutdown()
    }

    def getLevel1Var = complex.x must beEqualTo("complex1")
    def getLevel1bVar = complex.me must beEqualTo("complex")
    def getLevel2Var = complex.a must beEqualTo(1)
    def getLevel2bVar = complex.b must beEqualTo(2)
    def getLevel2cVar = complex.c must beEqualTo(3)

    def perfTest = timing("complex performance"){
        var x: ListBuffer[MatchResult[String]] = new ListBuffer[MatchResult[String]]()
        for (i <- 1 to 1000){
            val obj = Complex(s"complex-$i")
            val c = obj.save().toCC[Complex].get
            x.+=(c.x must beEqualTo(s"complex-$i"))
        }
        x.result().reduce(_ and _)
    }

}
