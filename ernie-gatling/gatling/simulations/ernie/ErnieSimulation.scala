package ernie

import io.gatling.core.scenario.configuration.Simulation
import io.gatling.com.ksmpartners.Predef._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._
import assertions._
import scala.concurrent.duration._
import io.gatling.core.Predef._
import com.ksmpartners.ernie.api.ErnieAPI
import com.ksmpartners.ernie.model.{ DefinitionEntity, ReportDefinitionMapResponse, ModelObject, ReportType }
import java.io.{ File, IOException }
import com.ksmpartners.ernie.util.Utility._
import scala.Predef._
import scala.Some
import assertions._
import io.gatling.core.session.EL
import com.ksmpartners.ernie.util.MapperUtility._
import scala.Some

/**
 * Created with IntelliJ IDEA.
 * User: acoimbra
 * Date: 6/21/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
class ErnieSimulation extends Simulation {
  def createTempDirectory(): File = {

    var temp: File = null

    temp = File.createTempFile("temp", System.nanoTime.toString)

    if (!(temp.delete())) {
      throw new IOException("Could not delete temp file: " + temp.getAbsolutePath)
    }

    if (!(temp.mkdir())) {
      throw new IOException("Could not create temp directory: " + temp.getAbsolutePath)
    }

    temp
  }

  lazy val api = {
    val defsDir = createTempDirectory
    org.apache.commons.io.FileUtils.copyFile(new File("test_def.rptdesign"), new File(defsDir, "test_Def.rptdesign"))
    org.apache.commons.io.FileUtils.copyFile(new File("test_def_params.rptdesign"), new File(defsDir, "test_def_params.rptdesign"))
    ErnieAPI(createTempDirectory.getAbsolutePath, defsDir.getAbsolutePath, createTempDirectory.getAbsolutePath, 5000L, 7, 14, 999)
  }

  def getADef(defsDir: File, defaultDef: String = "test_def.rptdesign"): String = try {
    defsDir.listFiles.apply(math.random.toInt % defsDir.listFiles.size).getAbsolutePath
  } catch {
    case _: Throwable => defaultDef
  }

  val writeSaml = "SAML vVbbkqJIEH33KwznscMGvLUarREFFLYXUBBRfNngUgJyUwoE/fpFaN22p3tmdh/2icgk89TJk3XJV6z5XqMPMEZR7IRBNfO9APcL76CWREE/1LCD+4HmI9yPjf4S8LN+45nsa7eU2ntOhgc1O44PfYJI0/Q5bT6HkUU0SJIiNvxsadjI12rVMTuoOeZfqK11mjuyXSfJJqq3yA5V1yljV2+Su24HNahWCzXzYIwTNA5wrAXxoNYgqWad7NTJF5l66ZNkn2w9dzrktlZVUIRzJnnIM1kbvpY1FcnRsPy8Eg/OVxP3l44VaHESoXf+5nf8SYLsEXmMiR3rR+2ei8xxsAsLk9GCMHAMzXMu2lUSHsV2aFaBZ4WRE9v+t8JQ5BW4jjKjblCt4EeNeKT2h0APDCOs1bGtUe9YEtqhCAUGqq6k8aD240/FL5LlSAvwLox8/Gj+O0YoOCEvPCCzjm+FvZP7c8AvtBq+IqM/Dgwvwc4JCdcdetAMhN/7iYw/h6ouIrRzspmD832W4Ss74iO9T2apButYCMf/pUUf2lOCKJqXoCGRdrXFnFiMDgxMj6THby4zZ64JJjceFAQ+BheOe3NL89O2vG+jMoNRFkDSwwzI+9HaeeF71GglAWa9h1ToR0py6ozFzlbhOcs/0FvafYMren8Aqrs32/O3KSHpiD91V8AQE1qcrWetQJgeZy1lC90MG+H5qLx0doeJ1gmwOjtnrmuZ+yYNhAvr0Egdj5BlUE/EhSPt9Okciy5sdkzNjVTONxVnszwE245jnjKzJaqDezkf+F9LmqLzvbxNm+yxWqzdDeZ6Ie3ycxijIT8eMyLLMEBzGRoigSZxyorqZBpux/bJEIAIIS2CdHuBMx64I0CtIG3zjKLwGbwAibYEhQaWzLiCXdFHnq83J4m2hhnHgmX505AZSrD1QLLHUMDqZmLzYpoylsoqojiFqSCb61YGWTAv47FMk720sl235RXZzVgZzMofvEy/SZ7RFNM32xB41kp5GVK87GZzmW+vrz658J3vvj0d8CJOK4xYrDaC6URZXaDM07AohaH5qdjgsLbengy/fVBluOLpcVlmxs9WDS4xR9ASG5lt+DBjWTCtlGxCGVACpe+hyINWCZbxb3KT9nRPkOULFHiACz/IeGiOlIvJMnRwAQJtuUfbdUa9lKQrucAcyEvf5yJbqpvLAkG0FA1i9dIe0Q0QT219tGwfHGfm+pPLEeJDpxWxmc5PnjI2OiMSCsSRWrXnu6eK5LJYEwjHcsFJoYxkAy151pE8bcctjNXmRcxYe0vbWOV6Bz2N+YvTAi2c9OwgG9srCekj6GfTde84EdTKBJNK2H4aW/ssgetgHwSK9SZj6sk/tkVVBi+wsz3PugxIIQDyY1lAFIuqLJFuLlJiVREvi6m8kFWnlxx0buNFYMqnMbdrzNet82m0Xp88IzWE5Xbiy5MukE6KOuehIuhYbs05Y5c0iAVSQ2A6LKgYozPBNbbJvruZTA6JKC1hZxGLRzph4dt+78/FCRkpRNT159SWid8k+7BtX1Rpb0tMDzH6PuspEadl5IRlKgS3S9eO5UdrUiXFQXmkPh+Tu7M8SMTHI/ZwBG+v6jLR98iIb+b11h2zVS6/FLX4+4GBeqYKj2PWd0VoseY71hWjNnw3bg91CTy8mZ+WZcLAdK4vLa4KYUyjHBT9akDIg+bBPAK7GEVFXOvLOOKGD5LYDpZxrpCPgrhamL+cQ7rN5rb2kJ1TjFEWf+VjvHx4yq/u4S/nK6NvXONQ2Z+bEF/ifPXz0Xmv5c4njiNHT2L0/Z/qtQeDGooCB0mhh2o/RRT38n38c34//9WdQkQD5UMjdvrx+YCu720f54CBVRum+ROK7sQflhn+5P6OcJIPpuWe+n/4rvA/8+XvORPf6098GsaHfwM="
  val readSaml = "SAML vVbJcuJIEL3zFQR9JLAWBEaEIaK0gFkkLCGB4TKhpbSANlQlJPj6EcJmjNvu7pnDnBSZynz18mUt+YSMMKD7ACGYYj+O6kUYRKhfeQeNLI36sYF81I+MEKI+tvpLIM379APZN95TGm85BRo0PIyTPkHkef6Qtx/i1CVokqSIV2m+tDwYGo36RBg0fPsv2jYg2+51Wz3WJlvMIwNbJv1ItyjShHTXeeySpl0GI5TBSYSwEeFBgyapdovstshHjer2O2y/wzy0e+y2UV/BFJVMypAHsjF8utZUJafD6+eJuHM+2ai/9N3IwFkK3/jb3/EnCZIlyhgb+e6Pxi0X2pPIiSuTN6I48i0j8M/GRRIJYi+26yBw49THXvitMBR5AW7BwmpZFBP9aBD31P4Q6I5hiowW8gzqDUuFDkxhZMG6rk4GjR9/Kn6VrKVGhJw4DdG9+e8YwegIgziBdgu9F/ZG7s8Bv9Bq+ASt/iSyggz5RyhfdmhiWBC99RNafw5Vf0mh4xdzH5X7rEAXdsRHep/MqxqC70KE/0uLPrTnCrIyggwOmYMIOgf1vPZUnyOpvJMvpsw23zbpfFAR+BhcOW7NvZqftuVtG10zLBZsoiOrOM89hek6azzuPQLTaLtHLsO87SPqgA8rilvrAB17RIJo6Tya5J7mZLlhJ7aBhSY1FULrpNrEEuc2o2XWPiO2gZ501upMC71Tm9mc8gMs4qOuMPTopDcFxtsus3HSbSZzpB8ny0JVcnau2+0oEbnefN1MnrWV4QguE+SH8CyRjjkZ3Mr5wP9S0gyebuW9dkhWMLBxM/jLheSU5xDDoTSZ8IrA88DY85wIZY5EuaBsprN4O/GOlgwUUeQUkG/P4lwC+zGgdJHzJH61kgrxDFTOlVcccDV+L3s1cxyEZnuaGWuxGAlgef1paTwle2akehNRRpvXqScpec67G2GlKDMxlzV7zRSiABbXeKRxJJvXtuuOppO9QtDA/PpD0rhnNbDaSv7sWbIkuLmkiZSk7YuFJnXWF59W+U43346LJAXlNV6pVhuL+XSln0VN4sSqFJ6TZgo9QsZ6e7TCTrLRRF3iJtcyC2mu06PMHouuQheeFYqFIIBZ7com1gAlU+ZOVCTAXMEK6Vlrc4EZyJp2FmUJoMoPCkm0x6uzLfBcdAYy5+4P3t4fsznJ1UqBR6AsfVeK7G72pSwiSJeKReiPnTFHAzzzzPGyk/j+fB9OzwcRJV0mFQpTmjYLIT1BUpSJA6V3Fk6zpu4FZMiE7+7BcUVZ2avoavOuGhjO6MXSXx+VQvC2nIc2IzYxcyydfQYwKGO9qJh4ugrNsRgWszV7mMqb2hSRq7jTnLi7IhPX0S6KVu6zhqhmeOgoGw08it3tad7jQS4CoN2XBRSlqspVuPZLTug15fwy0160jc9miTl6DVIwk3I8cujFmjkdx+v1MbByS15up6E27QH1uNosJHElm0hjFiPLyWjiBW5iYPsCqFnjEzGit9mu9zqdJpmiLsXuC1YOXCaIz7tduFCmZLoi0l64oLY8fla9ZNs5b9Sdp/Is5M1dwa7SkVGQU4GvESMnX/tumK7JDakMrkfq8zG5Oa8Hifh4xO6O4PuruszMHbTwu3m5dSdCfVReigb+fmCgHqjK49stpwqt1nzDumA0hm/G+0N9BR6+m5+W5ePI9i8vLarLMeZgCQp/NSCUQYtokQIHw7SKY76MI97xQYa9aIlLhUIY4Xpl/nIO6bDktnGXXVLEsMBf+figHJ7Kq3v4y/nK6luXOHjtz7sQX+J89fPeeavlxgfj1DczDL//U7/0YNCAaeRDNQ5g46eI6l6+jX/+7+e/ll+JaMFyaER+H58SeHlv+6gEjNzGMIWGfeN9t8rwJ/d3fLNyLr1uqf+Fro7+mS5/T5n4Xn3i0yg+/Bs="

  // val e = ernie(writeSaml, readSaml)
  var e = ernie(api)

  def scn(s: String) = {

    scenario(s)
      .exec(session => {
        session.set("postCount", range(1, 5))
          .set("resCount", range(4, 8))
          .set("defs", List())
      })
      .exec(e.createDef("test_def_params.rptdesign"))
      .exec(e.getDefs)
      .repeat(5) {
        randomSwitch(
          50 -> exec(e.postJob(Some("${defs(0)}"), ReportType.PDF)).exec(e.getResult(None)),
          50 -> exec(e.postJob(Some("${defs(0)}"), ReportType.PDF))).repeat(5) {
            exec(session => {
              val jobs = session.get[List[Long]]("jobs") getOrElse List.empty[Long]
              session.set("currentJob", util.Random.shuffle(jobs).headOption getOrElse (session.get[Long]("currentJob") getOrElse -1L))
            })
            randomSwitch(
              60 -> exec(e.getResult(None)),
              40 -> pause(1 second))
          }
      }
      .repeat("${postCount}") {
        randomSwitch(
          20 -> exec(e.deleteJob(None)),
          80 -> pause(0))
      }
  }

  // setUp(scn("ErnieHTTP").protocolConfig(httpConfig.baseURL("http://localhost:8080")).inject(ramp(100 users) over (30 seconds)))
  setUp(scn("ErnieAPI").inject(ramp(100 users) over (1 seconds)))

  def range(start: Int, end: Int): Int = start + (math.random.toInt % end)
}
