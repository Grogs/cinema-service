package me.gregd.cineworld.frontend

import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._
import me.gregd.cineworld.frontend.components.film.FilmPageComponent.{Date, Props, Today, Tomorrow}
import me.gregd.cineworld.frontend.components.film.FilmPageComponent
import me.gregd.cineworld.frontend.components.IndexPage

class Wiring(baseUrl: BaseUrl) {
  val routerConfig = RouterConfigDsl[Page].buildConfig{ dsl =>
    import dsl._
    def date = string("(today|tomorrow)").xmap[Date]{
      case "tomorrow" => Tomorrow
      case "today" | _ => Today
    }(_.key)
    (removeTrailingSlashes
      |staticRoute(root, Home) ~> renderR( rtr => IndexPage(rtr) )
      |dynamicRouteCT(("#!/films" / string("[0-9]+") / date).caseClass[Films]) ~> dynRenderR((p,r) => FilmPageComponent(Props(r, p)))
      ).notFound(redirectToPage(Home)(Redirect.Replace))
  }

  val router = Router(baseUrl, routerConfig)

}