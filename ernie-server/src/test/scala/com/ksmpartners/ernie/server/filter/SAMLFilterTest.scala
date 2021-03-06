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

package com.ksmpartners.ernie.server.filter

import org.testng.annotations.{ BeforeClass, Test }
import com.ksmpartners.ernie.server.PropertyNames._
import net.liftweb.mocks.{ MockHttpServletResponse, MockHttpServletRequest }
import javax.servlet.{ ServletResponse, ServletRequest, FilterChain }
import javax.servlet.http.HttpServletResponse
import java.io.{ ByteArrayOutputStream, FileOutputStream, FileInputStream, File }
import com.ksmpartners.ernie.util.Utility._
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder
import com.ksmpartners.ernie.util.Base64Util
import com.ksmpartners.ernie.server.filter.SAMLConstants._
import org.testng.Assert
import org.slf4j.{ LoggerFactory, Logger }
import com.ksmpartners.ernie.server.filter.SAMLFilter.SAMLHttpServletRequestWrapper
import com.ksmpartners.ernie.util.TestLogger

class SAMLFilterTest extends TestLogger {

  private val readMode = "read"
  private val writeMode = "write"
  private val readWriteMode = "read-write"
  private val log: Logger = LoggerFactory.getLogger("com.ksmpartners.ernie.server.filter.SAMLFilterTest")

  @BeforeClass
  def setup() {
    val ks = Thread.currentThread.getContextClassLoader.getResource("keystore.jks")
    System.setProperty(keystoreLocProp, ks.getPath)
  }

  @Test
  def goodAuthReturns200() {
    val filter = new SAMLFilter
    val req = new MockHttpServletRequest
    val resp = new MockResp
    val chain = new Chain

    req.headers += (authHeaderProp -> List(getSamlHeaderVal(readWriteMode)))

    filter.doFilter(req, resp, chain)

    Assert.assertEquals(resp.getStatusCode, 200)
  }

  @Test
  def noAuthReturns401() {
    val filter = new SAMLFilter
    val req = new MockHttpServletRequest
    val resp = new MockResp
    val chain = new Chain

    filter.doFilter(req, resp, chain)

    Assert.assertEquals(resp.getStatusCode, 401)
  }

  @Test
  def encodeRunToken() {
    log.info("readwriterun:")
    log.info(new String(encodeToken("read-write-run")))
  }

  @Test
  def canGetUserName() {
    val filter = new SAMLFilter
    val req = new MockHttpServletRequest
    val resp = new MockResp
    val chain = new Chain

    req.headers += (authHeaderProp -> List(getSamlHeaderVal(readWriteMode)))

    filter.doFilter(req, resp, chain)

    Assert.assertEquals(chain.userName, "readWriteUser")
  }

  def getSamlHeaderVal(mode: String): String = "SAML " + (new String(encodeToken(mode)))

  def encodeToken(mode: String): Array[Byte] = {
    val samlUrl = Thread.currentThread.getContextClassLoader.getResource("saml/" + mode + ".xml")
    val samlFile = new File(samlUrl.getFile)
    var bos: Array[Byte] = null

    var deflatedToken: Array[Byte] = null
    try_(new FileInputStream(samlFile)) { file =>
      val fileBytes: Array[Byte] = new Array[Byte](file.available())
      file.read(fileBytes)
      deflatedToken = new DeflateEncoderDecoder().deflateToken(fileBytes)
    }

    val encodedToken = Base64Util.encode(deflatedToken)

    try_(new ByteArrayOutputStream()) { os =>
      os.write(encodedToken)
      bos = os.toByteArray
    }

    bos
  }

  class MockResp extends MockHttpServletResponse(null, null) {
    def getStatusCode: Int = statusCode
  }

  class Chain extends FilterChain {
    var userName: String = ""
    def doFilter(request: ServletRequest, response: ServletResponse) {
      if (request.isInstanceOf[SAMLHttpServletRequestWrapper])
        userName = request.asInstanceOf[SAMLHttpServletRequestWrapper].getRemoteUser
    }
  }

}