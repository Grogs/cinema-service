package me.gregd.cineworld.dao.cinema.cineworld

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import fakes.{FakeRatings, NoOpCache}
import me.gregd.cineworld.config.values.TmdbKey
import me.gregd.cineworld.dao.TheMovieDB
import me.gregd.cineworld.dao.cinema.cineworld.raw.CineworldRepository
import me.gregd.cineworld.dao.movies.Movies
import me.gregd.cineworld.domain.{Cinema, Movie, Performance}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.ws.ahc.AhcWSClient
import stub.Stubs

class CineworldCinemaDaoTest extends FunSuite with ScalaFutures with Matchers {

  implicit val defaultPatienceConfig = PatienceConfig(Span(2000, Millis))

  val wsClient = AhcWSClient()(ActorMaterializer()(ActorSystem()))
  val tmdb = new TheMovieDB(TmdbKey(""), wsClient, Stubs.tmdb.baseUrl, NoOpCache.cache)
  val movieDao = new Movies(tmdb, FakeRatings)
  val cineworldRaw = new CineworldRepository(wsClient, NoOpCache.cache, Stubs.cineworld.baseUrl)
  val cineworld = new CineworldCinemaDao(movieDao, tmdb, cineworldRaw)

  test("retrieveCinemas") {
    val cinemas = cineworld.retrieveCinemas().futureValue.take(3)
    cinemas shouldEqual expectedCinemas
  }

  test("retrieveMoviesAndPerformances") {
    val showings = cineworld.retrieveMoviesAndPerformances("1010882", "2017-05-23").futureValue.take(3)
    pprint.pprintln(showings, width = 190)
    showings shouldEqual expectedShowings
  }

  val expectedCinemas = List(Cinema("1010804", "Aberdeen - Queens Links"), Cinema("1010808", "Aberdeen - Union Square"), Cinema("1010805", "Aldershot"))

  private val ticketBase = "https://www.cineworld.co.uk/ecom-tickets?siteId=1010882&prsntId"
  private val postBase = "https://www.cineworld.co.uk/xmedia-cw/repo/feats/posters"

  val expectedShowings = Map(
    Movie("Half Girlfriend (Hindi)", Some("HO00004553"), Some("default"), None, None, None, None, None, None, Some(s"$postBase/HO00004553.jpg")) -> List(
      Performance("20:00", true, "2D", s"$ticketBase=85595", Some("23/05/2017"))
    ),
    Movie("Whisky Galore!", Some("HO00004360"), Some("default"), None, None, None, None, None, None, Some(s"$postBase/HO00004360.jpg")) -> List(
      Performance("11:20", true, "2D", s"$ticketBase=85584", Some("23/05/2017")),
      Performance("15:20", true, "2D", s"$ticketBase=85674", Some("23/05/2017"))
    ),
    Movie("The Secret Scripture", Some("HO00004373"), Some("default"), None, None, None, None, None, None, Some(s"$postBase/HO00004373.jpg")) -> List(
      Performance("11:00", true, "2D", s"$ticketBase=85687", Some("23/05/2017")),
      Performance("13:30", true, "2D", s"$ticketBase=85688", Some("23/05/2017")),
      Performance("20:50", true, "2D", s"$ticketBase=85695", Some("23/05/2017"))
    )
  )

}
