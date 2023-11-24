val spinalVersion = "1.9.4"

lazy val root = (project in file("."))
  .settings(
    name := "I2C Gpio Expander",
    inThisBuild(
      List(
        organization := "com.github.spinalhdl",
        scalaVersion := "2.11.12",
        version := "2.0.0"
      )
    ),
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" % "spinalhdl-core_2.11" % spinalVersion,
      "com.github.spinalhdl" % "spinalhdl-lib_2.11" % spinalVersion,
      compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.11" % spinalVersion),
      "org.scalatest" %% "scalatest" % "3.2.5",
      "org.yaml" % "snakeyaml" % "1.8"
    ),
    Compile / scalaSource := baseDirectory.value / "hardware" / "scala",
    Test / scalaSource := baseDirectory.value / "test" / "scala"
  )
  .dependsOn(nafarr, zibal)

lazy val nafarr = RootProject(file("modules/elements/nafarr/"))
lazy val zibal = RootProject(file("modules/elements/zibal/"))

run / connectInput := true
fork := true
