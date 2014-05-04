package me.gregd.cineworld.domain

import me.gregd.cineworld.dao.cineworld.CineworldDao
import me.gregd.cineworld.dao.movies.MovieDao

case class Cinema(
  id: String,
  name: String
) {
  def getMovies(implicit dao: CineworldDao, imdb: MovieDao) = dao.getMovies(id)
}