package com.ansvia.graph

import com.tinkerpop.blueprints._

trait AbstractDbObject {
    def getVertex:Vertex
    def save()(implicit db:Graph):Vertex
    def delete()(implicit db:Graph)
    def isSaved:Boolean
    def reload()(implicit db: Graph):this.type
    def -->(label:String)(implicit db:Graph):EdgeWrapper

}
