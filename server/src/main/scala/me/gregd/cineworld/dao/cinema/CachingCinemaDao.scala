package me.gregd.cineworld.dao.cinema

import javax.inject.{Inject, Singleton}

import grizzled.slf4j.Logging
import me.gregd.cineworld.dao.cinema.cineworld.CineworldCinemaDao
import me.gregd.cineworld.domain._
import me.gregd.cineworld.util.Clock
import me.gregd.cineworld.util.Implicits.FutureOrElse
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

@Singleton
class CachingCinemaDao @Inject()(remoteCineworld: CineworldCinemaDao, scheduler: Scheduler, clock: Clock) extends CinemaDao with Logging {

  type Listings = Map[(String, String), Map[Movie, List[Performance]]]

  private var cinemas: Future[Seq[Cinema]] = remoteCineworld.retrieveCinemas()
  private var listings: Future[Listings] = fetchListings(cinemas)

  def run(): Unit = {
    val someMinutes = (1.5.hours.toSeconds + Random.nextInt(1.5.hours.toSeconds.toInt)).seconds
    scheduler.scheduleOnce(someMinutes) {
      refresh().foreach( _ => logger.info("Refresh completed"))
      run()
    }
  }

  logger.info("Scheduling refresh")
  scheduler.scheduleOnce(1.hour)(run())

  protected def fetchListings(eventualCinemas: Future[Seq[Cinema]]): Future[Listings] = {
    val days = (0 to 1).map(clock.today() plusDays _ toString)
    logger.info("Fetching listings")
    (
      for {
        cinemas <- eventualCinemas
      } yield
        for {
          cinema <- cinemas
          day <- days
          listings = remoteCineworld.retrieveMoviesAndPerformances(cinema.id, day)
          position = (cinemas.indexOf(cinema) + 1) * (days.indexOf(day) + 1)
          total = cinemas.size * days.size
          _ = listings.onFailure { case ex => logger.error(s"Failed to retrieve listings for ${cinema.id} / $day.", ex) }
        } yield
          for {
            ls <- listings
            _ = logger.debug(s"Retrieved listings for ${cinema.id} / $day.  Item $position of $total.")
          } yield ((cinema.id, day), ls)
    ).map(Future.sequence(_)).flatMap(identity).map(_.toMap)
  }

  protected def refreshCinemas() = {
    logger.info("Refreshing cinema")

    val next = for {
      nextCinemas <- remoteCineworld.retrieveCinemas()
      _ = logger.info("Retrieved cinemas")
    } yield nextCinemas

    next.onComplete {
      case Success(_) =>
        logger.info("Successfully refreshed cinemas")
        this.cinemas = next
      case Failure(ex) =>
        logger.error("Unable to refresh cinemas", ex)
    }

    next
  }

  protected def refreshListings(eventualCinemas: Future[Seq[Cinema]]) = {
    logger.info("Refreshing listings")

    val next = for {
      nextCinemas <- eventualCinemas
      nextListings <- fetchListings(cinemas)
      _ = logger.info("Fetched listings")
    } yield nextListings

    next.onComplete {
      case Success(_) =>
        logger.info("Successfully refreshed listings")
        this.listings = next
      case Failure(ex) =>
        logger.error("Unable to refresh listings", ex)
    }

    next
  }

  def refresh(): Future[Unit] = {
    refreshListings(refreshCinemas()).map(_ => ())
  }

  def retrieveCinemas() = cinemas orElse refreshCinemas()

  def retrieveMoviesAndPerformances(cinema: String, date: String) = {
    def forRequest(l: Listings) = l((cinema, date))
    (listings orElse refreshListings(cinemas)).map(forRequest)
  }

}