package me.gregd.cineworld.domain.service

import java.time.LocalDate

import com.typesafe.scalalogging.LazyLogging
import me.gregd.cineworld.domain.Cinema
import me.gregd.cineworld.domain.model.{Film, Performance}
import me.gregd.cineworld.domain.transformer.CineworldTransformer
import me.gregd.cineworld.integration.PostcodeIoIntegrationService
import me.gregd.cineworld.integration.cineworld.CineworldIntegrationService
import org.json4s._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CineworldService(
                          underlying: CineworldIntegrationService,
                          postcodeService: PostcodeIoIntegrationService
) extends LazyLogging {

  implicit val formats = DefaultFormats

  def retrieveCinemas(): Future[Seq[Cinema]] =
    for {
      rawCinemas <- underlying.retrieveCinemas()
      postcodes = rawCinemas.map(_.postcode)
      coordinates <- postcodeService.lookup(postcodes)
    } yield {
      rawCinemas.map(raw => CineworldTransformer.toCinema(raw, coordinates.get(raw.postcode)))
    }

  def retrieveMoviesAndPerformances(cinemaId: String, date: LocalDate): Future[Map[Film, Seq[Performance]]] = {

    underlying.retrieveListings(cinemaId, date).map { listingsBody =>
      logger.debug(s"Retrieving listings for $cinemaId:$date")
      val performancesById = listingsBody.events.groupBy(_.filmId)

      val res = for {
        raw <- listingsBody.films
        film = CineworldTransformer.toFilm(raw)
        performances = performancesById(raw.id).map(CineworldTransformer.toPerformances)
      } yield film -> performances

      res.toMap
    }
  }
}
