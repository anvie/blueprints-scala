package com.ansvia.graph

import org.specs2.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import org.specs2.matcher.MatchResult

import scala.collection.mutable.ListBuffer

/**
 * Author: robin
 *
 */
class DbObjectComplexSpec extends Specification {

    import com.ansvia.graph.testing.model._
    import BlueprintsWrapper._

    def is = s2"""$sequential ^
       "Complex DbObject inheritance should
            get level 1 var             ${getLevel1Var}
            get level 1b var            ${getLevel1bVar}
            get level 2 var             ${getLevel2Var}
            get level 2b var            ${getLevel2bVar}
            get level 2c var            ${getLevel2cVar}
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

}
