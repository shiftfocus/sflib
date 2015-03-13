package ca.shiftfocus.krispii.core.lib

import java.io.StringWriter
import java.io.PrintWriter
import play.api.Logger

object ExceptionWriter {
  def print(exception: Throwable): String = {
    var sw = new StringWriter()
    exception.printStackTrace(new PrintWriter(sw))
    sw.toString()
  }
}
