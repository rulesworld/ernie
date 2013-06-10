/**
 * This source code file is the intellectual property of KSM Technology Partners LLC.
 * The contents of this file may not be reproduced, published, or distributed in any
 * form, except as allowed in a license agreement between KSM Technology Partners LLC
 * and a licensee. Copyright 2012 KSM Technology Partners LLC.  All rights reserved.
 */

package com.ksmpartners.ernie.server

import net.liftweb.common.{ Box, Full }
import com.ksmpartners.ernie.server.filter.AuthUtil._
import com.ksmpartners.ernie.server.filter.SAMLConstants._

import net.liftweb.http._
import org.slf4j.LoggerFactory
import rest.RestHelper
import com.ksmpartners.ernie.model.ModelObject
import service.ServiceRegistry

/**
 * Object containing the stateless dispatch definition for an ernie server
 */
object DispatchRestAPI extends RestHelper with JsonTranslator {

  private val log = LoggerFactory.getLogger("com.ksmpartners.ernie.server.DispatchRestAPI")

  case class Resource(path: String, children: List[Resource]) //, requestTemplates: RequestTemplate*)

  private var tree: List[List[String]] = Nil

  private def traverse(r: Resource, path: List[String]) {
    if (r.children.isEmpty) tree = tree.::((path.::(r.path)).reverse)
    else {
      r.children.foreach(f => traverse(f, path.::(r.path)))
      tree = tree.::((path.::(r.path)).reverse)
    }
  }

  private val api: List[Resource] = List(Resource("jobs", List(
    Resource("catalog", List(
      Resource("", Nil))),
    Resource("", List(
      Resource("status", Nil),
      Resource("result",
        List(Resource("detail", Nil))))))),
    Resource("defs", List(
      Resource("", List(Resource("rptdesign", Nil))))))

  private def headFilter(f: () => Box[LiftResponse]): () => Box[LiftResponse] = { () =>
    {
      val respBox = f()
      val resp: LiftResponse = respBox.open_!
      val response = InMemoryResponse(Array(), resp.toResponse.headers, resp.toResponse.cookies, resp.toResponse.code)
      Full(response)
    }
  }
  /**
   * Method that verifies that the requesting user is in the given role
   * @param req - The request being handled
   * @param role - The role to verify
   * @param f - The function to be called if the user is in the role
   * @return the function f, or a ForbiddenResponse if the user is not in the specified role
   */
  private def authFilter(req: Req, role: String*)(f: () => Box[LiftResponse]): () => Box[LiftResponse] = {
    if (role.foldLeft(false)((b, s) => b || isUserInRole(req, s))) f else () => {
      log.debug("Response: Forbidden Response. Reason: User is not authorized to perform that action")
      Full(ForbiddenResponse("User is not authorized to perform that action"))
    }
  }

  /**
   * Method that verifies that the requesting user accepts the correct ctype
   * @param req - The request being handled
   * @param f - The function to be called if the user accepts the correct ctype
   * @return the function f, or a NotAcceptableResponse if the user does not accept the correct ctype
   */
  private def ctypeFilter(req: Req)(f: () => Box[LiftResponse]): () => Box[LiftResponse] = {
    if (acceptsErnieJson(req)) f else () => {
      log.debug("Response: Not Acceptable Response. Reason: Resource only serves " + ModelObject.TYPE_FULL)
      Full(NotAcceptableResponse("Resource only serves " + ModelObject.TYPE_FULL))
    }
  }

  private def idIsLongFilter(req: Req)(f: () => Box[LiftResponse]): () => Box[LiftResponse] = try {
    req.path(0).toLong
    f
  } catch {
    case _ => () => {
      log.debug("Response: Bad Response. Reason: Job ID provided is not a number")
      Full(ResponseWithReason(BadResponse(), ("Job ID provided is not a number: " + req.path(0))))
    }
  }

  def shutdown() {
    ServiceRegistry.shutdownResource.shutdown()
  }

  def init() {
    ServiceRegistry.init()

    api.map(f => traverse(f, Nil))

    serve("jobs" :: Nil prefix {
      case req@Req(Nil, _, PostRequest) => (authFilter(req, writeRole, runRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.post(req))
      case req@Req(Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.getMap("/jobs"))
      case req@Req(Nil, _, HeadRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply headFilter(() => ServiceRegistry.jobsResource.getMap("/jobs"))
      case req@Req("catalog" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.getCatalog)
      case req@Req("complete" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.getCatalog(Some("complete")))
      case req@Req("failed" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.getCatalog(Some("failed")))
      case req@Req("deleted" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.getCatalog(Some("deleted")))
      case req@Req("expired" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.getCatalog(Some("expired")))
      case req@Req("expired" :: Nil, _, DeleteRequest) => (authFilter(req, writeRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.jobsResource.purge())
      case req@Req(jobId :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_ compose idIsLongFilter(req)_) apply (() => ServiceRegistry.jobsResource.get(jobId))
      case req@Req(jobId :: "status" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_ compose idIsLongFilter(req)_) apply (() => ServiceRegistry.jobStatusResource.get(jobId))
      case req@Req(jobId :: "status" :: Nil, _, HeadRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_ compose idIsLongFilter(req)_) apply headFilter(() => ServiceRegistry.jobStatusResource.get(jobId))
      case req@Req(jobId :: "result" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose idIsLongFilter(req)_) apply (() => ServiceRegistry.jobResultsResource.get(jobId, Full(req)))
      case req@Req(jobId :: "result" :: Nil, _, HeadRequest) => (authFilter(req, readRole)_ compose idIsLongFilter(req)_) apply headFilter(() => ServiceRegistry.jobResultsResource.get(jobId, Full(req)))
      case req@Req(jobId :: "result" :: Nil, _, DeleteRequest) => (authFilter(req, writeRole)_ compose ctypeFilter(req)_ compose idIsLongFilter(req)_) apply (() => ServiceRegistry.jobResultsResource.del(jobId))
      case req@Req(jobId :: "result" :: "detail" :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_ compose idIsLongFilter(req)_) apply (() => ServiceRegistry.jobResultsResource.getDetail(jobId, Full(req)))
      case req@Req(jobId :: "result" :: "detail" :: Nil, _, HeadRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_ compose idIsLongFilter(req)_) apply headFilter(() => ServiceRegistry.jobResultsResource.getDetail(jobId, Full(req)))
    })

    serve("defs" :: Nil prefix {
      case req@Req(Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.defsResource.get("/defs"))
      case req@Req(Nil, _, HeadRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply headFilter(() => ServiceRegistry.defsResource.get("/defs"))
      case req@Req(Nil, _, PostRequest) => (authFilter(req, writeRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.defsResource.post(req))
      case req@Req(defId :: Nil, _, GetRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.defDetailResource.get(defId))
      case req@Req(defId :: Nil, _, HeadRequest) => (authFilter(req, readRole)_ compose ctypeFilter(req)_) apply headFilter(() => ServiceRegistry.defDetailResource.get(defId))
      case req@Req(defId :: "rptdesign" :: Nil, _, PutRequest) => (authFilter(req, writeRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.defDetailResource.put(defId, req))
      case req@Req(defId :: Nil, _, DeleteRequest) => (authFilter(req, writeRole)_ compose ctypeFilter(req)_) apply (() => ServiceRegistry.defDetailResource.del(defId))
    })

    tree.foreach(Path =>
      if (!Path.contains("")) {
        serve({
          case Req(Path, _, _) => Full(MethodNotAllowedResponse())
        })
      } else {
        val Path2 = Path.slice(Path.indexOf("") + 1, Path.length)
        serve(Path.slice(0, Path.indexOf("")) prefix {
          case Req(variable :: Path2, _, _) => Full(MethodNotAllowedResponse())
        })
      })

    serve {
      case req => {
        log.error("Got unknown request: {}", req)
        log.debug("Response: Not Found Response.")
        () => Full(NotFoundResponse())
      }
    }
  }

}
