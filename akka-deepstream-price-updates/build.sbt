name := "akka-deepstream-price-updates"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

resolvers += Resolver.jcenterRepo


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "io.deepstream" % "deepstream.io-client-java" % "2.2.2",
  "com.typesafe.akka" %% "akka-stream" % "2.5.3"
)