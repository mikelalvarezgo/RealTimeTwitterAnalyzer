package utils

trait Logger {


  lazy val logger = LoggerFactory.getLogger(getClass.getName)

}
