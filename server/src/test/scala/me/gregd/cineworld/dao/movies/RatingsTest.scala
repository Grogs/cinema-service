package me.gregd.cineworld.dao.movies

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import fakes.NoOpCache
import me.gregd.cineworld.config.values.OmdbKey
import me.gregd.cineworld.dao.ratings.{Ratings, RatingsResult}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.ws.ahc.AhcWSClient
import stub.Stubs

class RatingsTest extends FunSuite  with Matchers with ScalaFutures with IntegrationPatience {

  val wsClient = AhcWSClient()(ActorMaterializer()(ActorSystem()))
  val ratings = new Ratings(wsClient, NoOpCache.cache, Stubs.omdb.baseUrl, OmdbKey(""))

  test("get known ratings") {
    val RatingsResult(rating, votes, metascore, rottenTomatoes) = ratings.fetchRatings("tt0451279").futureValue

    rating shouldBe Some(8.3)
    votes shouldBe Some(61125)
    metascore shouldBe Some(76)
    rottenTomatoes shouldBe Some("93%")
  }

  test("ratings for invalid ID") {
    val RatingsResult(rating, votes, metascore, rottenTomatoes) = ratings.fetchRatings("invalid").futureValue

    rating shouldBe None
    votes shouldBe None
    metascore shouldBe None
    rottenTomatoes shouldBe None
  }

  test("ratings for movie without any ratings") {
    val RatingsResult(rating, votes, metascore, rottenTomatoes) = ratings.fetchRatings("tt0974015").futureValue

    rating shouldBe None
    votes shouldBe None
    metascore shouldBe None
    rottenTomatoes shouldBe None
  }

}
