package routes


@Api(value = "/gatherer", description = "Gatherer routes")
@Path("/gatherer")
class GathererRoutes(
  apiName:String,
  controller: PickupController) extends RESTRoute(apiName)
  with Logging{

}
