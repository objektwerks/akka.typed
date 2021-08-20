package typed

import akka.actor.ActorSystem

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.{ExecutionContext, Future}

object RestJokeService {
  implicit lazy val formats = DefaultFormats

  def getJoke(implicit system: ActorSystem, dispatcher: ExecutionContext): Future[String] = {
    val client = Http()
    client.singleRequest( HttpRequest(uri = "http://api.icndb.com/jokes/random/") ).flatMap { response =>
      Unmarshal(response)
        .to[String]
        .map { json => s"${parseJson(json)}" }
    }
  }

  def parseJson(json: String): String = {
    val jValue = parse(json)
    (jValue \ "value" \ "joke").extract[String]
  }
}