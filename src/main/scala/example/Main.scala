package example

import com.typesafe.scalalogging.LazyLogging
import sttp.client4.DefaultSyncBackend
import sttp.client4.quick.*
import sttp.model.Uri

import scala.util.{Failure, Success, Try}


object Main extends App with LazyLogging {
  println("\n V1: readable imperative")
  println(mainV1("https://api.ipify.org/?format=json"))
  println("\n V2: for comprehension")
  println(mainV2("https://api.ipify.org/?format=json"))
  println("\n Done.")

  case object Errors {
    //    val jsonError = raw"""{"ip": "-not a JSON-"}"""
    val jsonError = "-not a JSON-"
    val networkError = "-network error-"
    val parseError = "-URL parse error-"
  }

  def mainV2(url: String): Either[String, String] = {
    val parsedUri = Uri.parse(url)

    val result = {
      for {
        getUri <- parsedUri.toOption
        response <- Try {
          quickRequest.get(getUri).send()
        }.toOption
        _ = logger.info(s"response from ${getUri.toString}: ${response.show()}")
        responseBody = response.body

        result = Try {
          ujson.read(responseBody)("ip").str
        }.fold(_ => Left(Errors.jsonError), successResp => Right(successResp))

      } yield {
        result
      }
    }.getOrElse(Left(Errors.networkError))

    result
  }


  def mainV1(url: String): Either[String, String] = {
    // consume the following API
    val getUri = Uri.parse(url) match
      case Left(_) => return Left(Errors.parseError)
      case rv@Right(value) => value
    val tryRequest = Try {
      basicRequest.get(getUri)
    } match
      case Failure(exception) => return Left(Errors.networkError)
      case Success(value) => value

    val response = Try {
      tryRequest.send(DefaultSyncBackend())
    } match
      case Failure(exception) => return Left(Errors.networkError)
      case Success(value) => value
    logger.info(s"response from ${getUri.toString}: ${response.show()}")

    // parse the response into a JSON object
    val responseBody = response.body match
      case Left(value) => "---"
      case Right(value) => value

    val result = Try {
      ujson.read(responseBody)("ip").str
    }.fold(_ => Left(Errors.jsonError), successResp => Right(successResp))


    result
  }
}
