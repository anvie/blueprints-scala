package com.ansvia.graph

import org.specs2.mutable.Specification
import com.ansvia.graph.testing.model.ModelUsingAbstractIDGetter
import org.specs2.specification.Scope
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory

/**
 * Author: robin
 * Date: 11/4/13
 * Time: 1:19 PM
 *
 */
class AbstractIDGetterSpec extends Specification {
    args(sequential=true)

    import com.ansvia.graph.BlueprintsWrapper._

    implicit val db = TinkerGraphFactory.createTinkerGraph()

    class Ctx extends Scope {
        val v = ModelUsingAbstractIDGetter("zzz").save()
//        db.commit()
    }

    "Abstract ID getter" should {
        "using it" in new Ctx {
            v.toCC[ModelUsingAbstractIDGetter].get must beAnInstanceOf[ModelUsingAbstractIDGetter]
        }
    }

    step {
        db.shutdown()
    }

}


