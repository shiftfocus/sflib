package ca.shiftfocus.lib.exceptions

import java.io.StringWriter
import java.io.PrintWriter

object ExceptionWriter {
  def print(exception: Throwable): String = {
    var sw = new StringWriter()
    exception.printStackTrace(new PrintWriter(sw))
    sw.toString()
  }
}
