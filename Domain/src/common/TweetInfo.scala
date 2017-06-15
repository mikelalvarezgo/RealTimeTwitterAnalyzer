package common

import scala.util.Try
case class TweetInfo(
  tweet_id: Long,
  createdAt: Long,
  latitude: Option[Location],
  user_id: Long,
  user_name: String,
  user_followers: Int,
  tweetText: String,
  lenguage: String)

case class Location(
  latitude: Double,
  longitude:Double
)
object Location {
  lazy implicit val format = jsonFormat2(Location.apply)
  type Latitude = Double
  type Longitude = Double
}

object TweetInfo {

  lazy implicit val format =  jsonFormat8(TweetInfo.apply)
  type Tweet_id = Long
  type CreatedAt = Long
  type Geo_Location = Location
  type User_Id = Long
  type User_Name = String
  type User_Followers = Int

  def content2TwitterInfo(tw: Status,leng: String): Try[TweetInfo] = Try {
    val location = Try{
      val tweetLoc = tw.getGeoLocation
      Location(tweetLoc.getLatitude, tweetLoc.getLongitude)
    }.toOption

    println("LOCATION "+location.toString)
    TweetInfo(
      IDGenerator.next,
      tw.getCreatedAt.getTime,
      location,
      tw.getUser.getId,
      tw.getUser.getName,
      tw.getUser.getFollowersCount,
      tw.getText,
      leng)
  }
}


