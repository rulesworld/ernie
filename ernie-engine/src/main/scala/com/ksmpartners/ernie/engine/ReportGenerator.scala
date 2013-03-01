package com.ksmpartners.ernie.engine

import org.eclipse.birt.report.engine.api._
import org.eclipse.birt.core.framework.Platform

/**
 * Class used to generate BIRT reports
 */
class ReportGenerator(pathToDefinitions: String, pathToOutputs: String) {

  var engine: IReportEngine = null

  /**
   * Method to be called before any reports can be generated
   */
  def startup {
    val ec = new EngineConfig

    Platform.startup(ec)
    val factory = Platform.createFactoryObject( IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY )

    engine = (factory match {
      case fact: IReportEngineFactory => fact
      case _ => throw new ClassCastException
    }).createReportEngine(ec)

  }

  /**
   * Method that runs the .rtpdesign file at the given location rptDefName, and outputs the results to outputFileName
   * as a .pdf
   */
  def runPdfReport(rptDefName: String, outputFileName: String) {
    if (engine==null) throw new IllegalStateException("ReportGenerator was not started")
    val filePath = pathToDefinitions + "/" + rptDefName
    val design = engine.openReportDesign(filePath)
    val renderOption = new PDFRenderOption
    renderOption.setOutputFileName(pathToOutputs + "/" + outputFileName)
    renderOption.setOutputFormat("pdf")
    runReport(design, renderOption)
  }

  /**
   * Method that runs the .rtpdesign file at the given location rptDefName, and outputs the results to outputFileName
   * as .html
   */
  def runHtmlReport(rptDefName: String, outputFileName: String) {
    if (engine==null) throw new IllegalStateException("ReportGenerator was not started")
    val filePath = pathToDefinitions + "/" + rptDefName
    val design = engine.openReportDesign(filePath)
    val renderOption = new HTMLRenderOption
    renderOption.setOutputFileName(pathToOutputs + "/" + outputFileName)
    renderOption.setOutputFormat("html")
    runReport(design, renderOption)
  }

  private def runReport(design: IReportRunnable, option: RenderOption) {
    val task: IRunAndRenderTask = engine.createRunAndRenderTask(design)
    task.setRenderOption(option)
    task.run
    task.close
  }

  /**
   * Method to be called after all the reports have been run.
   */
  def shutdown {
    engine.destroy
    Platform.shutdown
    engine = null
  }

}
