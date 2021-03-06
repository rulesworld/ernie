/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ksmpartners.ernie.server.service

import com.ksmpartners.ernie.server.PropertyNames._
import com.ksmpartners.ernie.util.Utility._
import java.util.Properties
import java.io.{ FileInputStream, File }
import org.slf4j.{ LoggerFactory, Logger }
import com.ksmpartners.ernie.server.RequiresProperties
import com.ksmpartners.ernie.api._
import ErnieBuilder._
import java.util.concurrent.TimeUnit

/**
 * Object that registers the services used by the stateless dispatch
 */
object ServiceRegistry extends JobDependencies
    with DefinitionDependencies
    with RequiresAPI
    with RequiresProperties {

  private val log: Logger = LoggerFactory.getLogger("com.ksmpartners.ernie.server.ServiceRegistry")

  protected val properties: Properties = {

    val propsPath = System.getProperty(propertiesFileNameProp)

    if (null == propsPath) {
      throw new RuntimeException("System property " + propertiesFileNameProp + " is undefined")
    }

    val propsFile = new File(propsPath)
    if (!propsFile.exists) {
      throw new RuntimeException("Properties file " + propsPath + " does not exist.")
    }

    if (!propsFile.canRead) {
      throw new RuntimeException("Properties file " + propsPath + " is not readable; check file privileges.")
    }
    val props = new Properties()
    try_(new FileInputStream(propsFile)) { propsFileStream =>
      props.load(propsFileStream)
    }
    props
  }

  protected val ernie = {

    if (!properties.stringPropertyNames.contains(rptDefsDirProp)) {
      throw new RuntimeException("Properties file does not contain property " + rptDefsDirProp)
    }
    if (!properties.stringPropertyNames.contains(outputDirProp)) {
      throw new RuntimeException("Properties file does not contain property " + outputDirProp)
    }
    if (!properties.stringPropertyNames.contains(jobDirProp)) {
      throw new RuntimeException("Properties file does not contain property " + jobDirProp)
    }

    val jobDir = properties.get(jobDirProp).toString
    val rptDefsDir = properties.get(rptDefsDirProp).toString
    val outputDir = properties.get(outputDirProp).toString
    val workerCount = if (properties.stringPropertyNames.contains(workerCountProp)) properties.get(workerCountProp).toString.toInt else 5
    var to = 30 * 1000L

    if (properties.contains(requestTimeoutSeconds))
      to = properties.get(requestTimeoutSeconds).asInstanceOf[Long]

    val defaultRetentionDays: Int = try { properties.get(defaultRetentionPeriod).toString.toInt } catch { case e: Exception => 25 }
    val maximumRetentionDays: Int = try { properties.get(maximumRetentionPeriod).toString.toInt } catch { case e: Exception => 50 }

    ErnieEngine(ernieBuilder withFileReportManager (jobDir, rptDefsDir, outputDir)
      timeoutAfter (scala.concurrent.duration.FiniteDuration(to, TimeUnit.MILLISECONDS))
      withDefaultRetentionDays (defaultRetentionDays)
      withMaxRetentionDays (maximumRetentionDays)
      withWorkers (workerCount)
      build ()).start

  }

  val jobsResource = new JobsResource
  val jobStatusResource = new JobStatusResource
  val jobEntityResource = new JobEntityResource
  val jobResultsResource = new JobResultsResource

  val defsResource = new DefsResource
  val defDetailResource = new DefDetailResource

  /**
   * Empty method. Calling instantiates this object.
   */
  def init() {
    log.info("BEGIN Initializing ServiceRegistry...")
    log.info("Loaded properties: {}", properties.toString)
    log.info("END Initializing ServiceRegistry...")
  }

  def shutDown() = ernie.shutDown()

}
