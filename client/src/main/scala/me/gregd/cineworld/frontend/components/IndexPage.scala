package me.gregd.cineworld.frontend.components

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.TagMod.Composite
import japgolly.scalajs.react.vdom.html_<^._
import me.gregd.cineworld.domain.{Cinema, CinemaApi}
import me.gregd.cineworld.frontend.components.film.FilmPageComponent.Today
import me.gregd.cineworld.frontend.services.Geolocation
import me.gregd.cineworld.frontend.{Client, Films, Page}
import org.scalajs.dom.experimental.permissions.PermissionName.geolocation
import org.scalajs.dom.experimental.permissions.PermissionState.granted
import org.scalajs.dom.experimental.permissions._
import org.scalajs.dom.raw.Position
import org.scalajs.dom.window.navigator

import scala.concurrent.Future.successful
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.Try
import scalacss.ScalaCssReact.scalacssStyleaToTagMod

object IndexPage {
  import me.gregd.cineworld.frontend.components.{IndexStyle => styles}
  val label = "label".reactAttr

  sealed trait Loadable[+T]
  case object Unloaded extends Loadable[Nothing]
  case object Loading extends Loadable[Nothing]
  case class Loaded[T](value: T) extends Loadable[T]

  case class State(allCinemas: Loadable[Map[String, Map[String, Seq[Cinema]]]], nearbyCinemas: Loadable[Seq[Cinema]])

  type Props = RouterCtl[Page]

  def apply(router: Props): Unmounted[Props, State, Backend] = component(router)

  val component = ScalaComponent
    .builder[Props]("IndexPage")
    .initialState(State(Unloaded, Unloaded))
    .renderBackend[Backend]
    .componentDidMount(_.backend.initialise())
    .build

  class Backend($ : BackendScope[Props, State]) {

    def initialise() = {
      val noop = successful(CallbackTo.pure(Option(())))
      val loadCinemasIfPermissioned = Callback.future(Geolocation.havePermission().map(loadNearbyCinemas().when(_)).fallbackTo(noop))
      Callback.sequence(
        Seq(
          loadCinemasIfPermissioned,
          loadAllCinemas()
        ))
    }

    def loadAllCinemas() = Callback.future {
      for {
        cinemas <- Client[CinemaApi].getCinemas().call()
      } yield $.modState(_.copy(allCinemas = Loaded(cinemas)))
    }

    def loadNearbyCinemas() = $.modState(_.copy(nearbyCinemas = Loading)) >> Callback.future {
      for {
        userLocation <- Geolocation.getCurrentPosition()
        nearbyCinemas <- Client[CinemaApi].getNearbyCinemas(userLocation).call()
      } yield $.modState(_.copy(nearbyCinemas = Loaded(nearbyCinemas)))
    }

    def selectCinema(e: ReactEventFromInput) = {
      val cinemaId = e.target.value
      for {
        p <- $.props
        _ <- p.set(Films(cinemaId, Today))
      } yield ()
    }

    def render(state: State) = {

      val cinemaDropdowns = state.allCinemas match {
        case Unloaded | Loading =>
          <.div(styles.blurb, "Loading")
        case Loaded(cinemas) =>
          Composite(
            for {
              (typ, cinemas) <- cinemas.toVector
            } yield
              <.div(
                <.select(
                  styles.selectWithOffset,
                  ^.id := "cinemas",
                  ^.`class` := ".flat",
                  ^.onChange ==> selectCinema,
                  <.option(^.value := "?", ^.selected := "selected", ^.disabled := true, typ),
                  Composite(for { (groupName, cinemas) <- cinemas.toVector.reverse } yield
                    <.optgroup(label := groupName, Composite(for (cinema <- cinemas.toVector) yield <.option(^.value := cinema.id, cinema.name))))
                )
              ))
      }

      val nearbyCinemas = <.div(
        state.nearbyCinemas match {
          case Unloaded =>
            <.button(
              styles.btn,
              ^.onClick --> loadNearbyCinemas,
              "Load Nearby Cinemas"
            )
          case Loading =>
            <.div(^.color.white, ^.textAlign.center, <.i(^.`class` := s"fa fa-refresh fa-spin fa-5x"))
          case Loaded(cinemas) =>
            <.select(
              styles.selectWithOffset,
              ^.id := "nearby-cinemas",
              ^.`class` := ".flat",
              ^.onChange ==> selectCinema,
              <.option(^.value := "?", ^.selected := "selected", ^.disabled := true, "Select nearby cinema..."),
              Composite(for (c <- cinemas.toVector) yield <.option(^.value := c.id, c.name))
            )

        }
      )

      <.div(
        ^.id := "indexPage",
        <.div(
          styles.top,
          <.div(styles.title, "Fulfilmed"),
          <.div(styles.blurb, "See films showing at your local cinema, with inline movie ratings and the ability to sort by rating."),
          nearbyCinemas,
          cinemaDropdowns
        ),
        <.div(
          styles.description,
          ^.id := "description"
        )
      )
    }

  }

}
