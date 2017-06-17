package me.gregd.cineworld.dao.movies

import javax.inject.{Inject, Singleton}

import com.rockymadden.stringmetric.similarity.DiceSorensenMetric
import com.typesafe.scalalogging.slf4j.LazyLogging
import me.gregd.cineworld.dao.TheMovieDB
import me.gregd.cineworld.dao.model.TmdbMovie
import me.gregd.cineworld.domain.{Film, Format, Movie}
import monix.execution.FutureUtils.extensions._
import monix.execution.Scheduler.Implicits.global
import org.json4s._

import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class Movies @Inject()(tmdb: TheMovieDB, ratings: Ratings) extends MovieDao with LazyLogging {

  implicit val formats = DefaultFormats

  def toMovie(film: Film): Future[Movie] = {
    logger.debug(s"Creating movie from $film")

    for {
      movieOpt <- find(film.cleanTitle)
      movie = movieOpt.getOrElse(Movie(film.title))
      format = Format.split(film.title)._1
      poster = movie.posterUrl orElse Option(film.poster_url).filter( ! _.isEmpty)
    } yield
      movie.copy(
        title = film.title,
        cineworldId = Option(film.id),
        format = Option(format),
        posterUrl = poster
      )

  }

  private var cachedMovies: Future[Seq[Movie]] = Future.failed(new UninitializedError)
  private var lastCached: Long = 0
  def allMoviesCached() = {
    def refresh = {
      logger.info(s"refreshing movies cache, old value:\n$cachedMovies")
      cachedMovies = allMovies()
      lastCached = System.currentTimeMillis()
    }
    val age = System.currentTimeMillis() - lastCached
    this.synchronized(
      if (age.millis > 10.hours) refresh
    )
    cachedMovies
  }.timeoutTo(5.seconds, Future.successful(Seq.empty))

  private def ratingAndVotes(imdbId: Option[String]): Future[(Option[Double], Option[Int])] = {
    imdbId
      .map(ratings.ratingAndVotes)
      .getOrElse(Future.successful(None))
      .map(sequence)
  }

  private def allMovies(): Future[Seq[Movie]] = {
    collapse(
      for {
        nowPlaying <- tmdb.fetchNowPlaying()
      } yield for {
        tmdbMovie <- nowPlaying
        tmdbId = tmdbMovie.id.toString
      } yield for {
        alternateTitles <- tmdb.alternateTitles(tmdbId)
        imdbId <- tmdb.fetchImdbId(tmdbId)
        (rating, votes) <- ratingAndVotes(imdbId)
      } yield for {
        altTitle <- (tmdbMovie.title :: alternateTitles).distinct
      } yield
        toMovie(tmdbMovie.copy(title = altTitle), tmdbId, imdbId, rating, votes)
    )
  }

  private def toMovie(tmdbMovie: TmdbMovie, tmdbId: String, imdbId: Option[String], rating: Option[Double], votes: Option[Int]) = {
    Movie(
      tmdbMovie.title,
      None,
      None,
      imdbId,
      Option(tmdbId),
      rating,
      votes,
      Option(tmdbMovie.vote_average),
      Option(tmdbMovie.vote_count.toInt),
      tmdbMovie.poster_path.map(tmdb.baseImageUrl + _)
    )
  }

  val compareFunc: (String, String) => Double =
    DiceSorensenMetric(1).compare(_: String, _: String).get

  val minWeight = 0.8

  def find(title: String): Future[Option[Movie]] = for (allMovies <- allMoviesCached()) yield {
    val matc = allMovies.maxBy(m => compareFunc(title, m.title))
    val weight = compareFunc(title, matc.title)
    logger.debug(s"Best match for $title was  ${matc.title} ($weight) - ${if (weight > minWeight) "ACCEPTED" else "REJECTED"}")
    if (weight > minWeight) Option(matc) else None
  }

  private def sequence[A, B](ot: Option[(A, B)]): (Option[A], Option[B]) = {
    ot match {
      case None => None -> None
      case Some((a, b)) => Option(a) -> Option(b)
    }
  }

  private def collapse[T](fsfsT: Future[Seq[Future[Seq[T]]]]): Future[Seq[T]] =
    fsfsT.map( sfsT => Future.sequence(sfsT).map(_.flatten)).flatMap(identity)

}
