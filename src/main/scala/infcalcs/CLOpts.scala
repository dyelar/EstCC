package infcalcs

/**
 * Case class whose fields comprise the required and optional
 * command line options for channel capacity calculation.  
 * 
 * For more details see the usage text with the flag "--help"
 */
case class Config(
    verbose: Boolean = false, 
    dataFile: String = "", 
    paramFile: String = "",
    seed: Int = -1)

/**
 * Parser for command line options, inherited by [[EstCC]]
 */
trait CLOpts {
  val parser = new scopt.OptionParser[Config]("EstCC.jar") {
    head("EstCC", "1.0")
    opt[Unit]('v', "verbose") action { (_, c) =>
      c.copy(verbose = true)
    } text ("periodically print calculation status " +
      "to stdout")
    opt[String]('d', "data") required () valueName ("<file>") action {
      (x, c) => c.copy(dataFile = x)
    } text ("input data for calculation")
    opt[String]('p', "parameters") valueName ("<file>") action {
      (x, c) => c.copy(paramFile = x)
    } text ("modify default parameters" +
      " with optional file")
    opt[Int]('s', "seed") action {
      (x, c) => c.copy(seed = x)
    }
    help("help") text ("prints this usage text")
  }
}