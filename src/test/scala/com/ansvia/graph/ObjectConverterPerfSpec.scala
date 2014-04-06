package com.ansvia.graph

import org.specs2.Specification
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory
import com.tinkerpop.blueprints.Vertex
import org.specs2.specification.Step
import scala.collection.mutable.ArrayBuffer

/**
 * Author: robin
 * Date: 12/31/12
 * Time: 5:21 AM
 *
 */



class ObjectConverterPerfSpec extends Specification {

    import com.ansvia.graph.testing.model._

    
    private def timing[T](title:String)(func: => T):T = {
        val ts = System.currentTimeMillis()
        val rv = func
        val ends = System.currentTimeMillis() - ts
        println(title + " done in " + ends + "ms")
        rv
    }
    
    
    def is = sequential ^
        "Object cenverter performance" ^
        p ^
            "serialize simple" ! oc.serializeSimple ^
            "serialize complex" ! oc.serializeComplex ^
            "deserialize simple" ! oc.deserializeSimple ^
            "deserialize complex" ! oc.deserializeComplex ^
            Step(oc.close()) ^
    end

    object oc {

        implicit val db = TinkerGraphFactory.createTinkerGraph()

        var ccsSimple = ArrayBuffer[AbstractDbObject]()
        var ccsComplex = ArrayBuffer[AbstractDbObject]()

        timing("generating data"){
            for (i <- 1 to 5000){
                val animal = Animal("animal" + i)
                animal.age = 10 + i
                animal.kind = "kind" + i
                animal.save()
                ccsSimple += animal

                val complex = Complex("complex" + i)
                complex.a = i * 2
                complex.b = i * 3
                complex.c = i * 4
                complex.me = "complex_me" + i
                complex.save()
                ccsComplex += complex
            }
        }
        
        def serializeSimple = {
            val rvAnimal = ArrayBuffer[Vertex]()

            timing("serialize simple"){
                for (cc <- ccsSimple){
                    rvAnimal += ObjectConverter.serialize[Vertex](cc, cc.getVertex, false)
//                    println(x)
                }
            }
            rvAnimal.length must_== 5000
        }


        def serializeComplex = {

            val rvComplex = ArrayBuffer[Vertex]()

            timing("serialize complex"){
                for (cc <- ccsComplex){
                    rvComplex += ObjectConverter.serialize[Vertex](cc, cc.getVertex, false)
                }
            }
            rvComplex.length must_== 5000


        }


        def deserializeSimple = {
            val animals = ArrayBuffer[Animal]()
            timing("deserialize simple"){
                for (cc <- ccsSimple){
                    animals += ObjectConverter.deSerialize[Animal](cc.getVertex)
                }
            }
            animals.length must_== 5000
        }
        
        def deserializeComplex = {
            val complex = ArrayBuffer[Complex]()
            timing("deserialize complex"){
                for (cc <- ccsComplex){
                    complex += ObjectConverter.deSerialize[Complex](cc.getVertex)
                }
            }
            complex.length must_== 5000
        }


        def close(){
            db.shutdown()
        }
        
    }




}
