
ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val Zio_Version = "1.0.8"
val zio_Logging = "0.5.14"
val zio_Config  = "1.0.0"
val log4j_Version      = "2.13.3"



lazy val root = (project in file("."))
  .settings(
    name := "zio-payroll",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % Zio_Version,
      "dev.zio" %% "zio-logging"       % zio_Logging,
      "dev.zio" %% "zio-logging-slf4j" % zio_Logging,
      "dev.zio" %% "zio-config-magnolia" % zio_Config,
      "dev.zio" %% "zio-config-typesafe" % zio_Config,
      "org.apache.logging.log4j" % "log4j-core"       % log4j_Version,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j_Version,
      "com.lmax"                 % "disruptor" % "3.4.2",
      "dev.zio" %% "zio-test" % Zio_Version % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
