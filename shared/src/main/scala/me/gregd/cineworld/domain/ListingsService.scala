package me.gregd.cineworld.domain
import me.gregd.cineworld.domain.model.{Movie, Performance}

import scala.concurrent.Future

trait ListingsService {

  def getMoviesAndPerformancesFor(cinemaId: String, date: String): Future[Map[Movie, Seq[Performance]]]
}
