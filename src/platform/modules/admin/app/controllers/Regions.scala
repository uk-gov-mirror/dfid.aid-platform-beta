package controllers.admin

import play.api.mvc.{Action, Controller}
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import uk.gov.dfid.common.api.Api
import uk.gov.dfid.common.models.{Region, Country}
import reactivemongo.bson.{BSONString, BSONDocument, BSONObjectID}
import com.google.inject.Inject
import concurrent.ExecutionContext.Implicits.global
import controllers.traits.admin.Secured

class Regions @Inject()(val api: Api[Region]) extends Controller with Secured  {

  val form = Form(
    mapping(
      "id"    -> ignored[Option[BSONObjectID]](None),
      "code"  -> nonEmptyText,
      "title" -> nonEmptyText
    )(Region.apply)
     (Region.unapply)
  )

  // GET /regions
  def index = SecuredAction { user => request =>
    Async {
      api.all.map { regions =>
        Ok(views.html.admin.regions.index(regions))
      }
    }
  }

  // GET /regions/new
  def create = SecuredAction { user => request =>
    Ok(views.html.admin.regions.view(None, form))
  }

  // POST /regions
  def save = SecuredAction { user => implicit request =>
    form.bindFromRequest.fold(
      errors => BadRequest(views.html.admin.regions.view(None, errors)),
      country => Async {
        api.query(BSONDocument("code" -> BSONString(country.code))).map {
          case Nil => {
            api.insert(country)
            Redirect(routes.Regions.index)
          }
          case _ => {
            val errors = form.fill(country).withError("code", "Country code must be unique")
            BadRequest(views.html.admin.regions.view(None, errors))
          }
        }
      }
    )
  }

  // GET /regions/:id/edit
  def edit(id: String) = SecuredAction { user => request =>
    Async {
      api.get(id).map { maybeRegion =>
        maybeRegion.map { region =>
          Ok(views.html.admin.regions.view(Some(id), form.fill(region)))
        } getOrElse {
          Redirect(routes.Regions.index)
        }
      }
    }
  }

  // POST /regions/:id
  def update(id: String) = SecuredAction { user => implicit request =>
    form.bindFromRequest.fold(
      errors => BadRequest(views.html.admin.regions.view(Some(id), errors)),
      country => {
        api.update(id, country)
        Redirect(routes.Regions.index)
      }
    )
  }

  // POST /regions/:id/delete
  def delete(id: String) = SecuredAction { user => request =>
    api.delete(id)
    Redirect(routes.Regions.index)
  }

}
