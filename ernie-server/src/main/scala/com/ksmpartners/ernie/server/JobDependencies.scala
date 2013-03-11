/**
 * This source code file is the intellectual property of KSM Technology Partners LLC.
 * The contents of this file may not be reproduced, published, or distributed in any
 * form, except as allowed in a license agreement between KSM Technology Partners LLC
 * and a licensee. Copyright 2012 KSM Technology Partners LLC.  All rights reserved.
 */

package com.ksmpartners.ernie.server

import net.liftweb.common.{ Box, Full, Empty }
import net.liftweb.http.{ BadResponse, LiftResponse, PlainTextResponse, OkResponse }
import com.ksmpartners.ernie.engine.{ StatusRequest => SReq, ReportRequest => RReq, ShutDownRequest, Notify, Coordinator, ReportGenerator }
import com.ksmpartners.ernie.model.{ Notification, ReportRequest }
import com.fasterxml.jackson.databind.ObjectMapper
import net.liftweb.util.Props
import java.io.IOException

trait JobDependencies {

  val coordinator = new Coordinator(Props.get("rpt.def.dir").open_!, Props.get("output.dir").open_!).start()

  class JobsResource extends JsonTranslator {
    def get = {
      Full(OkResponse())
    }
    def put(body: Box[Array[Byte]]) = {
      var req: ReportRequest = null
      try {
        req = deserialize(body.open_!, classOf[ReportRequest])
        val response = (coordinator !! RReq(req.getReportDefId)).apply().asInstanceOf[Notify]
        getJsonResponse(new Notification(response.jobId, response.jobStatus))
      } catch {
        case e: IOException => Full(BadResponse())
      }
    }
  }

  class JobStatusResource extends JsonTranslator {
    def get(jobId: String) = {
      val response = (coordinator !! SReq(jobId.toLong)).apply().asInstanceOf[Notify]
      getJsonResponse(new Notification(response.jobId, response.jobStatus))
    }
  }

  class JobResultsResource {
    def get(jobId: String) = {
      Full(OkResponse())
    }
  }

  class ShutdownResource {
    def shutdown() {
      coordinator ! ShutDownRequest
    }
  }

}

/**
 * Trait containing method for serializing/deserializing JSONs
 */
trait JsonTranslator {
  private val mapper = new ObjectMapper

  /**
   * Serializes an object into a JSON String
   */
  def serialize[T](obj: T): String = {
    mapper.writeValueAsString(obj)
  }

  /**
   * Deserializes the given JSON String into an object of the type clazz represents
   */
  def deserialize[T](json: String, clazz: Class[T]): T = {
    mapper.readValue(json, clazz) match {
      case t: T => t
      case _ => throw new ClassCastException
    }
  }

  /**
   * Deserializes the given JSON Array[Byte] into an object of the type clazz represents
   */
  def deserialize[T](json: Array[Byte], clazz: Class[T]): T = {
    mapper.readValue(json, clazz) match {
      case t: T => t
      case _ => throw new ClassCastException
    }
  }

  /**
   * Serializes the given response object into a Full[PlainTextResponse] with a content-type of application/json and
   * an HTTP code of 200
   */
  def getJsonResponse[T](response: T): Box[LiftResponse] = {
    Full(PlainTextResponse(serialize(response), List(("Content-Type", "application/json")), 200))
  }
}