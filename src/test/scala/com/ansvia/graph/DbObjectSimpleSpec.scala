package com.ansvia.graph

import org.specs2.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import com.ansvia.graph.Exc.NotBoundException
import org.specs2.specification.Step

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
        "DbObject non tx should" ^
        p ^
            "able to reload" ! treesNonTx.reload ^
            "change property and save" ! treesNonTx.changeProperty ^
            "use getId via IDGetter" ! treesNonTx.useGetId ^
            "use getId from IdDbObject" ! treesNonTx.useGetIdDbObject ^
            "able to delete" ! treesNonTx.delete ^
            "get id immediately after save" ! treesNonTx.getIdAfterSave ^
            Step(treesNonTx.close()) ^
        p ^
        "DbObject tx should" ^
        p ^
            "able to reload" ! treesTx.reload ^
            "change property and save" ! treesTx.changeProperty ^
            "use getId via IDGetter" ! treesTx.useGetId ^
            "use getId from IdDbObject" ! treesTx.useGetIdDbObject ^
            "able to delete" ! treesTx.delete ^
            "get id immediately after save" ! treesNonTx.getIdAfterSave ^
            Step(treesTx.close()) ^
        end

    object treesNonTx {
        implicit val db = TinkerGraphFactory.createTinkerGraph()

        val dboDraft = SimpleDbo("a", "b")
        val dbo = dboDraft.save().toCC[SimpleDbo].get

        val dbo2Draft = IdSimpleDbo("b", "c")
        val dbo2 = dbo2Draft.save().toCC[IdSimpleDbo].get

        def close(){
            db.shutdown()
        }

        def reload = {
            dbo.b = "c"
            val dbo2 = dbo.reload()
            dbo2.a must beEqualTo("a") and(dbo2.b must beEqualTo("b")) and (dbo2.b must not equalTo "c")
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

        def useGetIdDbObject = {
            val d = db.getVertex(dbo2.getVertex.getId).toCC[IdSimpleDbo].get
            d.getId must beEqualTo(dbo2.getId)
        }

        def delete = {
            dbo.delete()
            dbo.isSaved must beEqualTo(false)
        }

        def getIdAfterSave = {
            val o = SimpleDbo("d", "f")
            o.save()

            (o.getId must not be throwAn[NotBoundException]) and
                (o.getId must not be equalTo(null)) and
                (o.isSaved must beTrue)
        }
    }

    object treesTx extends TitanBackedDb {
//        implicit val db = new Neo4jGraph("/tmp/neo4jdb-test-simple")

        val dboDraft = SimpleDboLong("a", "b")
        val dbo = transact {
            dboDraft.save().toCC[SimpleDboLong].get
        }


        val dbo2Draft = IdSimpleDboLong("b", "c")
        val dbo2 = transact {
            dbo2Draft.save().toCC[IdSimpleDboLong].get
        }


        def close(){
            db.shutdown()
        }

        def reload = {
            dbo.b = "c"
            val dbo2 = dbo.reload()
            dbo2.a must beEqualTo("a") and(dbo2.b must beEqualTo("b")) and (dbo2.b must not equalTo "c")
        }

        def changeProperty = {
            dbo.b = "d"
            dbo.save()
            db.commit()
            db.getVertex(dbo.getId).toCC[SimpleDboLong].get.b must beEqualTo("d")
        }

        def useGetId = {
            val v = db.getVertex(dbo.getVertex.getId)
            v.getId must beEqualTo(dbo.getId)
        }

        def useGetIdDbObject = {
            val d = db.getVertex(dbo2.getVertex.getId).toCC[IdSimpleDboLong].get
            d.getId must beEqualTo(dbo2.getId)
        }

        def delete = {
            dbo.delete()
            dbo.isSaved must beEqualTo(false)
        }

        def getIdAfterSave = {
            val o = SimpleDboLong("d", "f")
            o.save()
            db.commit()

            (o.getId must not be throwAn[NotBoundException]) and
                (o.getId must not be equalTo(null)) and
                (o.isSaved must beTrue)
        }
    }



}
