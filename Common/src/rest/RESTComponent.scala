package rest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.google.common.base.Charsets
import com.google.common.io.Resources

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.Source
import scala.util.Try
import scala.reflect.runtime.universe._

/**
  * A [[RESTComponent]] just holds a REST API.
  */
trait RESTComponent extends Swagger {
  thisComponent: Service with Logging with FileConfig =>

  val apiTypes: Seq[Type]
  val version: String = Meta.version

  implicit val system: ActorSystem
  implicit lazy val executor: ExecutionContextExecutor = system.dispatcher
  implicit lazy val materializer = ActorMaterializer()

  protected val apiName = thisComponent.getClass.getPackage.getName.split("\\.").last

  val routes: Route
  val bindingInterface: String
  val bindingPort: Int
  lazy val swaggerBindingInterface: String =
    getProperty[String](s"$apiName.common.http.swagger-interface").toOption.getOrElse(bindingInterface)

  implicit val exceptionHandler = ExceptionHandler {
    case e: Throwable =>
      extractUri { uri =>
        logger.error(s"Request to $uri could not be handled normally", e)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = e.getMessage))
      }
  }

  /**
    * Lazy evaluation of current HTTP service binding.
    * It includes the custom routes and the default one (showing the module version).
    * And also swagger and swagger UI!
    */
  lazy val binding: Http.ServerBinding = {
    Await.result(
      Http().bindAndHandle(
        routes ~ defaultRoute ~ swaggerRoutes,
        bindingInterface,
        bindingPort),
      Duration.Inf)
  }

  /**
    * The default route just prints out the version of the uService.
    */
  val defaultRoute: Route = {
    logRequestResult(apiName) {
      path("version") {
        complete {
          val pckg = thisComponent.getClass.getPackage
          Try {
            s"${pckg.getImplementationTitle}-${pckg.getImplementationVersion}"
          }.recover {
            case _ => s"$apiName-development"
          }.get
        }
      } ~ swaggerRoutes
    }
  }

  lazy val swaggerDoc = SwaggerDocService(
    system,
    apiTypes,
    s"$swaggerBindingInterface:$bindingPort",
    apiVersion = version,
    serviceName = apiName)

  //  Static content for swagger ui
  lazy val swaggerUi = {
    val scala.util.Success(httpSwaggerInterface) =
      getProperty[String](s"$name.common.http.swagger-interface")
    val scala.util.Success(httpPort) =
      getProperty[Int](s"$name.common.http.port")
    val contentLines = Resources.toString(
      Resources.getResource("swagger-ui/index.html"), Charsets.UTF_8).split("\n")
    //val contentLines = Source.fromFile(new File(s"conf/swagger-ui/index.html")).getLines()
    val content = contentLines.map {
      case line if line.contains("""url = """") =>
        s"""        url = "http://$httpSwaggerInterface:$httpPort/api-docs/swagger.json\";"""
      case line => line
    }.mkString("\n")
    HttpEntity(ContentTypes.`text/html(UTF-8)`, content)
  }

  /**
    * Required routes for auto-document the deployed REST API.
    */
  lazy val swaggerRoutes =
    path("swagger") {
      get {
        complete {
          swaggerUi
        }
      }
    } ~
      pathPrefix("webjars") {
        getFromResourceDirectory("META-INF/resources/webjars")
      } ~
      swaggerDoc.routes

  object Rest {
    def initialize: Http.ServerBinding = {
      swaggerDoc.export().map { path =>
        logger.info(s"Documentation was exported to $path")
      }.recover {
        case ex => logger.error(s"Couldn't export documentation [${ex.getMessage}]", ex)
      }.get
      binding
    }

    def stop: Unit = Await.result(binding.unbind(), Duration.Inf)
  }

}
