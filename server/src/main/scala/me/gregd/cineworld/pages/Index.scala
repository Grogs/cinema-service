package me.gregd.cineworld.pages

import scalatags.Text.all._
import scalatags.Text.tags2.{title => titleElem}

object Index {
  def apply(scriptPaths: List[String]) =
    html(
      head(
        titleElem("Fulfilmed")
//        ,
//        link(rel:="stylesheet", href:="webjars/font-awesome/4.5.0/css/font-awesome.min.css")
      ),
      body(
        style := "margin: 0",
        div(id := "content"),
        for (p <- scriptPaths) yield script(`type` := "text/javascript", src := p)
      )
    )

}
