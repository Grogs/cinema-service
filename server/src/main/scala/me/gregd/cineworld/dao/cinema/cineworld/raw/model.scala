package me.gregd.cineworld.dao.cinema.cineworld.raw

package object model {
  private val Postcode = ".* ([A-Z]{1,2}[0-9]{1}[A-Z0-9]? [0-9][A-Z]{2})$".r
  case class CinemaResp(id: String, displayName: String, address: String) {
    def postcode: String = {
      val Postcode(res) = address
      res
    }
  }
  case class MovieResp(BD: Seq[Day], code: String, TYP: Seq[String], n: String, TYPD: Seq[String])
  case class Day(date: String, P: Seq[Showing], d: Long)
  case class Showing(dt: Long, dub: Short, sub: Short, sold: Boolean, code: String, vn: String, is3d: Boolean, dattr: String, time: String, attr: String, vt: Int)
}
