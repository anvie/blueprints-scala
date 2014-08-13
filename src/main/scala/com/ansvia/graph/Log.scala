package com.ansvia.graph

import org.slf4j.LoggerFactory

/**
 * Author: robin
 * Date: 2/24/14
 * Time: 11:27 PM
 *
 */
private[graph] trait Log {

    final lazy val log = LoggerFactory.getLogger(getClass)

    def error(msg:String){
        log.error(msg)
    }

    def info(msg:String){
        log.info(msg)
    }

    def warn(msg:String){
        log.warn(msg)
    }


}
