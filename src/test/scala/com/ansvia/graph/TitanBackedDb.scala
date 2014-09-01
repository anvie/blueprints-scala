package com.ansvia.graph

//import com.lambdazen.bitsy.BitsyGraph
//import java.nio.file.Paths
import com.thinkaurelius.titan.core.TitanFactory
import org.apache.commons.configuration.BaseConfiguration
import java.io.File
import com.tinkerpop.blueprints.{TransactionalGraph, KeyIndexableGraph}


/**
 * Author: robin
 * Date: 9/1/14
 * Time: 2:22 PM
 *
 */

trait TitanBackedDb {
    implicit def db = TitanBackedDb.db
}

object TitanBackedDb {

    type GraphType = KeyIndexableGraph with TransactionalGraph

    val dirTls = new ThreadLocal[String]()
    val dbTls = new ThreadLocal[GraphType]()

    def db:GraphType = {

        val _db = dbTls.get()

        if (_db != null){
            _db
        }else{
            val dir = "/tmp/digaku2-test-" + Thread.currentThread().getId
            dirTls.set(dir)

            // clean up old data files
            val fDir = new File(dir)
            if (fDir.exists()){
                for ( f <- fDir.listFiles() )
                    f.delete()
                fDir.delete()
            }

            val dbConf = new BaseConfiguration
            dbConf.setProperty("storage.backend", "berkeleyje")
            dbConf.setProperty("storage.directory", dir)
            dbConf.setProperty("storage.transactions", false)
            dbConf.setProperty("storage.db-cache", true)
            dbConf.setProperty("storage.db-cache-time", 60000)

            val db = TitanFactory.open(dbConf)
            dbTls.set(db)

            db
        }
    }

}