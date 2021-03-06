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
import javax.servlet._
import java.io.{ ByteArrayOutputStream, FileInputStream, File }
import com.ksmpartners.ernie.util.Utility._
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder
import com.ksmpartners.ernie.util.Base64Util
import com.ksmpartners.ernie.server.filter.SAMLConstants._
import org.testng.Assert
import org.slf4j.{ LoggerFactory, Logger }
import com.ksmpartners.ernie.server.filter.SAMLFilter.SAMLHttpServletRequestWrapper
import com.ksmpartners.ernie.util.TestLogger
import net.liftweb.http.Req
import java.util

class FilterWrapperTest extends TestLogger {

  private val log: Logger = LoggerFactory.getLogger("com.ksmpartners.ernie.server.filter.FilterWrapperTest")
  private val readWriteMode = "read-write"

  @BeforeClass
  def setup() {
    val ks = Thread.currentThread.getContextClassLoader.getResource("keystore.jks")
    System.setProperty(keystoreLocProp, ks.getPath)
  }

  @Test
  def goodAuthReturns200() {
    val req = new MockHttpServletRequest
    val resp = new MockResp
    val filter = new FilterWrapper
    val chain = new Chain

    filter.init(new FilterConfig {
      def getFilterName: String = "FilterWrapper"

      def getInitParameterNames: util.Enumeration[_] = null

      def getInitParameter(p1: String): String = ""

      def getServletContext: ServletContext = null
    })

    req.headers += (authHeaderProp -> List(getSamlHeaderVal(readWriteMode)))

    filter.doFilter(req, resp, chain)

    Assert.assertEquals(resp.getStatusCode, 200)

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