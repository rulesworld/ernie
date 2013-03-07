/**
 * This source code file is the intellectual property of KSM Technology Partners LLC.
 * The contents of this file may not be reproduced, published, or distributed in any
 * form, except as allowed in a license agreement between KSM Technology Partners LLC
 * and a licensee. Copyright 2012 KSM Technology Partners LLC.  All rights reserved.
 */

package com.ksmpartners.ernie.engine

import actors.Actor
import com.ksmpartners.ernie.model.JobStatus

case class ReportRequest(rtpDefId: String)
case class JobRequest(rtpDefId: String, jobId: Int, self: Actor)
case class StatusRequest(jobId: Int)
case class Notify(jobId: Int, jobStatus: JobStatus, self: Actor)
