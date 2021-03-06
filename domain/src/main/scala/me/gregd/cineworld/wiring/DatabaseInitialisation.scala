package me.gregd.cineworld.wiring

import com.typesafe.scalalogging.LazyLogging
import me.gregd.cineworld.config._
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.migration.api._

object DatabaseInitialisation extends LazyLogging {

  type DB = PostgresProfile.backend.DatabaseDef

  implicit val dialect: PostgresDialect = new PostgresDialect()

  def createListings(listingsTableName: ListingsTableName): DBIO[Unit] = DBIO.seq(
    sqlu"""
      create table if not exists #${listingsTableName.value} (
        cinema_id text not null,
        date text not null,
        listings text not null,
        modified TIMESTAMPTZ not null default now(),
        primary key (cinema_id, date)
      )
    """
  )

  val createCinemas: DBIO[Unit] = DBIO.seq(
    sqlu"""
        create table if not exists cinemas (
          id varchar,
          chain varchar not null,
          json varchar not null,
          modified TIMESTAMPTZ not null default now(),
          primary key (id)
        )
    """
  )

  def migrate(listingsTableName: ListingsTableName): DBIO[Unit] =
    DBIO.seq(
      createListings(listingsTableName),
      createCinemas
    )
}
