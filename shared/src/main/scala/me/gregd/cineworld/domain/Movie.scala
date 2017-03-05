package me.gregd.cineworld.domain

case class Movie (
  title: String,
  cineworldId: Option[String],
  format: Option[String],
  imdbId: Option[String],
  tmdbId: Option[Double],
  rating: Option[Double],
  votes: Option[Int],
  tmdbRating: Option[Double],
  tmdbVotes: Option[Int],
  posterUrl: Option[String]
)

object Format {
  def split(title:String) = {
    title.take(5) match {
      case "2D - " | "(2D) " => ("2D",title.substring(5))
      case "3D - " => ("3D",title.substring(5))
      case _       => ("default",title)
    }
  }
}