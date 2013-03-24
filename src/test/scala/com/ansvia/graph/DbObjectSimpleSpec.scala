package com.ansvia.graph

import org.specs2.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory

/**
 * Author: robin
 * Date: 1/16/13
 * Time: 12:58 AM
 * 
 */
class DbObjectSimpleSpec extends Specification {

    import com.ansvia.graph.testing.model._
    import BlueprintsWrapper._

    def is = sequential ^
        "DbObject should" ^
        p ^
            "able to reload" ! dboTrees.reload ^
            "change property and save" ! dboTrees.changeProperty ^
            "use getId via IDGetter" ! dboTrees.useGetId ^
            "able to delete" ! dboTrees.delete ^
        end

    object dboTrees {
        implicit val db = TinkerGraphFactory.createTinkerGraph()

        val dboDraft = SimpleDbo("a", "b")
        val dbo = dboDraft.save().toCC[SimpleDbo].get

        def close(){
            db.shutdown()
        }

        def reload = {
            dbo.b = "c"
            val dbo2 = dbo.reload()
            dbo2.a must beEqualTo("a") and(dbo2.b must beEqualTo("b")) and (dbo2.b must not equalTo("c"))
        }

        def changeProperty = {
            dbo.b = "d"
            dbo.save()
            db.getVertex(dbo.getId).toCC[SimpleDbo].get.b must beEqualTo("d")
        }
        
        def useGetId = {
            val v = db.getVertex(dbo.getVertex.getId)
            v.getId must beEqualTo(dbo.getId)
        }

        def delete = {
            dbo.delete()
            dbo.isSaved must beEqualTo(false)
        }
    }

}
