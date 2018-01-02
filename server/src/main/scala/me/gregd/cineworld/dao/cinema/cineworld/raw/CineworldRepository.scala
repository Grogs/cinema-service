package me.gregd.cineworld.dao.cinema.cineworld.raw

import javax.inject.{Inject,Singleton}

import me.gregd.cineworld.Cache
import me.gregd.cineworld.config.CineworldConfig
import me.gregd.cineworld.dao.cinema.cineworld.raw.model._
import play.api.libs.json.Json
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalacache.memoization._

@Singleton
class CineworldRepository @Inject()(ws: WSClient, cache: Cache, config: CineworldConfig) {

  implicit val _ = cache.scalaCache

  implicit val d = Json.format[Showing]
  implicit val c = Json.format[Day]
  implicit val b = Json.format[MovieResp]
  implicit val a = Json.format[CinemaResp]

  private def curlCinemas(): Future[String] = memoize(1.day) {
    ws.url(s"${config.baseUrl}/getSites?json=1&max=200")
      .get()
      .map(_.body)
  }

  private def curl7DayListings(cinema: String): Future[String] = memoize(6.hours) {
    val url = s"${config.baseUrl}/pgm-site?si=$cinema&max=365"
    ws.url(url)
      .get()
      .map(_.body)
  }

  def retrieveCinemas(): Future[Seq[CinemaResp]] = {
    curlCinemas().map( r => Json.parse(r).as[Seq[CinemaResp]])
  }

  def retrieve7DayListings(cinema: String): Future[Seq[MovieResp]] = {
    curl7DayListings(cinema).map(r => Json.parse(r).as[Seq[MovieResp]])
  }

}

