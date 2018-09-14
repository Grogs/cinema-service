package me.gregd.cineworld.domain.repository

import java.time.LocalDate

import docker.Postgres
import me.gregd.cineworld.domain.model.{Movie, Performance}
import me.gregd.cineworld.wiring.ListingsTableName
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSuite, Matchers}
import slick.jdbc.PostgresProfile.api._

import scala.util.Random

class SlickListingsRepositoryTest extends FunSuite with Postgres with ScalaFutures with IntegrationPatience with Matchers {

  def randomTableName() = ListingsTableName("listings-" + Random.nextLong())

  val exampleMovies = Seq(
    Movie("Duck Duck Goose", Some("ho00005039"), None, None, None, None, None, None, None, Some("blah.jpg"), None, None) -> List(
      Performance("10:40", true, "2D", "https://www.cineworld.co.uk//ecom-tickets?tid=8112&prsntId=46903", Some("2018-04-12")),
      Performance("13:10", true, "2D", "https://www.cineworld.co.uk//ecom-tickets?tid=8112&prsntId=46904", Some("2018-04-12")),
      Performance("15:30", true, "2D", "https://www.cineworld.co.uk//ecom-tickets?tid=8112&prsntId=46905", Some("2018-04-12"))
    )
  )

  test("create") {
    val db = Database.forURL(postgresUrl)
    val repo = new SlickListingsRepository(db, randomTableName())
    repo.create().futureValue
  }

  test("persist") {

    val db = Database.forURL(postgresUrl)
    val repo = new SlickListingsRepository(db, randomTableName())


    val eventualAssertion = for {
      _ <- repo.create()
      _ <- repo.persist("test", LocalDate.now())(exampleMovies)
    } yield succeed

    eventualAssertion.futureValue
  }

  test("fetch") {

    val db = Database.forURL(postgresUrl)
    val repo = new SlickListingsRepository(db, randomTableName())


    val eventualAssertion = for {
      _ <- repo.create()
      _ <- repo.persist("test", LocalDate.now())(exampleMovies)
      output <- repo.fetch("test", LocalDate.now())
    } yield exampleMovies shouldEqual output

    eventualAssertion.futureValue
  }

}
