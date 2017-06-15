name := "RealTimeTwitterAnalicer"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

lazy val common = project.
  settings(Common.settings: _*)

lazy val api = project.
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.apiDependencies)

lazy val gatherer = project.
  dependsOn(common).
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.gathererDependencies)

lazy val domain = project.
  dependsOn(api).
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.domainDependencies)


lazy val root = (project in file(".")).
  aggregate(api, common, gatherer, domain)
