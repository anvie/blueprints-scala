package com.ansvia.graph

import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import org.specs2.Specification
import org.specs2.specification.Step

/**
 * Test verifying if optional values are stored correctly.
 * Author: pdolega
 */
class DbObjectOptionalSpec extends Specification {

    import com.ansvia.graph.testing.model._

    def is = sequential ^
        "DbObject non tx should" ^
        p ^
            "save NONE property" ! treesNonTx.getOptionalEntityNone ^
            "save SOME property" ! treesNonTx.getOptionalWithValue ^
            "reload Option attribute" ! treesNonTx.reload ^
            "overwrite with None" ! treesNonTx.overwriteWithNone ^
            Step(treesNonTx.close()) ^
        end


    object treesNonTx {
        import BlueprintsWrapper._

        implicit val db = TinkerGraphFactory.createTinkerGraph()

        def close(){
            db.shutdown()
        }

        def getOptionalEntityNone = {
            val o = IdSimpleDboOption(a="d", b="f")
            o.save()

            val a: String = o.getVertex.getProperty("a")
            val b: String = o.getVertex.getProperty("b")

            a must be equalTo("a")
            b must be equalTo("b")

            val optNull: String = o.getVertex.getProperty("c")

            optNull must beNull
        }

        def getOptionalWithValue = {
            val o = IdSimpleDboOption(a="d", b="f", opt=Some("opt"))
            o.save()

            val opt: String = o.getVertex.getProperty("opt")
            opt must beEqualTo("opt")
        }

        def reload = {
            val noneDbo = IdSimpleDboVarOption(opt=None, a="d", b="f")
            val noneDboSave = noneDbo.save().toCC[IdSimpleDboVarOption].get

            noneDboSave.opt = Some("test")
            val dboReload1 = noneDboSave.reload()
            dboReload1.opt must beNone

            val someDbo = IdSimpleDboVarOption(opt=Some("test"), a="d", b="f")
            val someDboSave = someDbo.save().toCC[IdSimpleDboVarOption].get

            someDboSave.opt = None
            val dboReload2 = someDboSave.reload()
            dboReload2.opt must beEqualTo(Some("test"))
        }

        def overwriteWithNone = {
            val someDbo = IdSimpleDboVarOption(opt=Some("some value"), a="d", b="f")
            val someDboSave = someDbo.save().toCC[IdSimpleDboVarOption].get
            someDboSave.opt must beEqualTo(Some("some value"))

            someDboSave.opt = None
            val noneDboSave = someDboSave.save().toCC[IdSimpleDboVarOption].get
            noneDboSave.opt must beEqualTo(None)

            val reloadedNoneDbo = noneDboSave.reload()
            reloadedNoneDbo.opt must beEqualTo(None)
        }
    }

}
