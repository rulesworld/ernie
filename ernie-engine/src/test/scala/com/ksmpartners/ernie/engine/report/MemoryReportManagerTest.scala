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
 *
 *
 */

package com.ksmpartners.ernie.engine.report

import org.testng.annotations.{ BeforeMethod, Test }
import org.testng.Assert
import com.ksmpartners.ernie.model.{ ReportEntity, DefinitionEntity, ReportType }
import scala.collection._
import org.joda.time.DateTime
import org.slf4j.{ LoggerFactory, Logger }
import com.ksmpartners.ernie.util.TestLogger
import java.io.{ ByteArrayInputStream, File }

class MemoryReportManagerTest extends TestLogger {

  private var reportManager: MemoryReportManager = new MemoryReportManager
  private val log: Logger = LoggerFactory.getLogger("com.ksmpartners.ernie.engine.report.MemoryReportManagerTest")

  @BeforeMethod
  def setup() {
    reportManager = new MemoryReportManager
    reportManager.putDefinition("def_1", "DEF_1".getBytes, new DefinitionEntity(DateTime.now(), "def_1", "default", null, "", null, null))
    reportManager.putDefinition("def_2", "DEF_2".getBytes, new DefinitionEntity(DateTime.now(), "def_2", "default", null, "", null, null))
    reportManager.putDefinition("def_3", "DEF_3".getBytes, new DefinitionEntity(DateTime.now(), "def_3", "default", null, "", null, null))
    reportManager.putDefinition("def_4", "DEF_4".getBytes, new DefinitionEntity(DateTime.now(), "def_4", "default", null, "", null, null))
    reportManager.putDefinition("def_5", "DEF_5".getBytes, new DefinitionEntity(DateTime.now(), "def_5", "default", null, "", null, null))
    reportManager.putReport("rpt_1", "RPT_1".getBytes, new ReportEntity(DateTime.now(), DateTime.now(), "rpt_1", "def_1", "default", null, ReportType.PDF, null, null))
    reportManager.putReport("rpt_2", "RPT_2".getBytes, new ReportEntity(DateTime.now(), DateTime.now(), "rpt_2", "def_2", "default", null, ReportType.PDF, null, null))
    reportManager.putReport("rpt_3", "RPT_3".getBytes, new ReportEntity(DateTime.now(), DateTime.now(), "rpt_3", "def_3", "default", null, ReportType.PDF, null, null))
    reportManager.putReport("rpt_4", "RPT_4".getBytes, new ReportEntity(DateTime.now(), DateTime.now(), "rpt_4", "def_4", "default", null, ReportType.PDF, null, null))
    reportManager.putReport("rpt_5", "RPT_5".getBytes, new ReportEntity(DateTime.now(), DateTime.now(), "rpt_5", "def_5", "default", null, ReportType.PDF, null, null))
  }

  @Test
  def testPutReport() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.rptId -> "rpt_6")
    entity += (ReportManager.sourceDefId -> "def_6")
    entity += (ReportManager.reportType -> ReportType.CSV)
    entity += (ReportManager.createdUser -> "default")
    var params = new mutable.HashMap[String, String]()
    params += ("PARAM_1" -> "VAL_1")
    params += ("PARAM_2" -> "VAL_2")
    params += ("PARAM_3" -> "VAL_3")
    entity += (ReportManager.paramMap -> params)
    val bosR = reportManager.putReport(entity)
    Assert.assertFalse(reportManager.hasReport("rpt_6"))
    bosR.close()
    Assert.assertTrue(reportManager.hasReport("rpt_6"))

    val report = reportManager.getReport("rpt_6").get
    Assert.assertNotNull(report.getParams)
    Assert.assertNotNull(report.getCreatedDate)
    Assert.assertNotNull(report.getRetentionDate)
  }

  @Test
  def testPutDefinition() {
    var entity = new mutable.HashMap[String, Any]()
    entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.defId -> "def_6")
    entity += (ReportManager.createdUser -> "default")
    val paramList = List("PARAM_1", "PARAM_2", "PARAM_3")
    entity += (ReportManager.paramNames -> paramList)
    val put = reportManager.putDefinition(entity)

    val bosD = put._2
    Assert.assertFalse(reportManager.hasDefinition(put._1.getDefId))
    bosD.close()
    Assert.assertTrue(reportManager.hasDefinition(put._1.getDefId))

    val definition = reportManager.getDefinition(put._1.getDefId).get
    Assert.assertNotNull(definition.getParamNames)
    Assert.assertNotNull(definition.getCreatedDate)

    val (dE, s) = reportManager.putDefinition(new DefinitionEntity(DateTime.now(), "def_6", "default", null, "", null, null))
    val file = new File(Thread.currentThread.getContextClassLoader.getResource("test_def.rptdesign").getPath)
    val xl = scala.xml.XML.loadFile(file)
    val len = xl.toString.length
    org.apache.commons.io.CopyUtils.copy(new ByteArrayInputStream(xl.toString.getBytes), s)
    s.close()
    val res = reportManager.getDefinitionContent(dE.getDefId)
    Assert.assertTrue(res.isDefined)
    try {
      val xml = scala.xml.XML.load(res.get)
      Assert.assertEquals(xml.toString.length, len)
    } catch {
      case _ => Assert.assertTrue(false)
    }
  }

  @Test
  def testUpdateDefinition() {
    var defn = reportManager.getDefinition("def_5")
    Assert.assertTrue(defn.isDefined)
    var entity = defn.get.getEntity
    val prev = entity.getCreatedUser
    entity.setCreatedUser(prev + "1")
    reportManager.updateDefinitionEntity("def_5", entity)
    defn = reportManager.getDefinition("def_5")
    Assert.assertTrue(defn.get.getCreatedUser == prev + "1")

  }

  @Test()
  def testGet() {
    val buf: Array[Byte] = new Array(5)
    reportManager.getDefinitionContent("def_1").get.read(buf)
    Assert.assertEquals(buf, "DEF_1".getBytes)
    reportManager.getReportContent("rpt_1").get.read(buf)
    Assert.assertEquals(buf, "RPT_1".getBytes)
  }

  @Test
  def testHas() {
    Assert.assertTrue(reportManager.hasDefinition("def_1"))
    Assert.assertTrue(reportManager.hasDefinition("def_2"))
    Assert.assertTrue(reportManager.hasDefinition("def_3"))
    Assert.assertTrue(reportManager.hasDefinition("def_4"))
    Assert.assertTrue(reportManager.hasDefinition("def_5"))
    Assert.assertTrue(reportManager.hasReport("rpt_1"))
    Assert.assertTrue(reportManager.hasReport("rpt_2"))
    Assert.assertTrue(reportManager.hasReport("rpt_3"))
    Assert.assertTrue(reportManager.hasReport("rpt_4"))
    Assert.assertTrue(reportManager.hasReport("rpt_5"))
    Assert.assertFalse(reportManager.hasReport("def_1"))
    Assert.assertFalse(reportManager.hasReport("def_2"))
    Assert.assertFalse(reportManager.hasReport("def_3"))
    Assert.assertFalse(reportManager.hasReport("def_4"))
    Assert.assertFalse(reportManager.hasReport("def_5"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_1"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_2"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_3"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_4"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_5"))
  }

  @Test(dependsOnMethods = Array("testUpdateDefinition"))
  def testDelete() {
    reportManager.deleteDefinition("def_1")
    reportManager.deleteDefinition("def_2")
    reportManager.deleteDefinition("def_3")
    reportManager.deleteDefinition("def_4")
    reportManager.deleteDefinition("def_5")
    reportManager.deleteReport("rpt_1")
    reportManager.deleteReport("rpt_2")
    reportManager.deleteReport("rpt_3")
    reportManager.deleteReport("rpt_4")
    reportManager.deleteReport("rpt_5")
    Assert.assertFalse(reportManager.hasReport("def_1"))
    Assert.assertFalse(reportManager.hasReport("def_2"))
    Assert.assertFalse(reportManager.hasReport("def_3"))
    Assert.assertFalse(reportManager.hasReport("def_4"))
    Assert.assertFalse(reportManager.hasReport("def_5"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_1"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_2"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_3"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_4"))
    Assert.assertFalse(reportManager.hasDefinition("rpt_5"))
  }

  @Test
  def testGetAll() {
    Assert.assertEquals(reportManager.getAllDefinitionIds.sortWith({ (x, y) => x < y }),
      List("def_1", "def_2", "def_3", "def_4", "def_5"))
    Assert.assertEquals(reportManager.getAllReportIds.sortWith({ (x, y) => x < y }),
      List("rpt_1", "rpt_2", "rpt_3", "rpt_4", "rpt_5"))
  }

  @Test
  def missingReportOrDefinitionReturnsNone() {
    Assert.assertEquals(reportManager.getReport("FAIL"), None)
    Assert.assertEquals(reportManager.getDefinition("FAIL"), None)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def missingDefIdThrowsException() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.createdUser -> "default")
    reportManager.putDefinition(entity)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def missingDefCreatedUserThrowsException() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.defId -> "def_6")
    reportManager.putDefinition(entity)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def missingRptIdThrowsException() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.sourceDefId -> "def_6")
    entity += (ReportManager.reportType -> ReportType.CSV)
    entity += (ReportManager.createdUser -> "default")
    reportManager.putReport(entity)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def missingSourceDefIdThrowsException() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.rptId -> "rpt_6")
    entity += (ReportManager.reportType -> ReportType.CSV)
    entity += (ReportManager.createdUser -> "default")
    reportManager.putReport(entity)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def missingReportTypeThrowsException() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.rptId -> "rpt_6")
    entity += (ReportManager.sourceDefId -> "def_6")
    entity += (ReportManager.createdUser -> "default")
    reportManager.putReport(entity)
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def missingRptCreatedUserThrowsException() {
    var entity = new mutable.HashMap[String, Any]()
    entity += (ReportManager.rptId -> "rpt_6")
    entity += (ReportManager.sourceDefId -> "def_6")
    entity += (ReportManager.reportType -> ReportType.CSV)
    reportManager.putReport(entity)
  }

}
