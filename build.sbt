organization := "com.antonwierenga"

name := "activemq-cli"

version := "0.0.2"

scalaVersion := "2.11.6"

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

libraryDependencies += "org.springframework.shell" % "spring-shell" % "1.1.0.RELEASE"
libraryDependencies += "org.apache.activemq" % "activemq-all" % "5.13.1"
libraryDependencies += "com.typesafe" % "config" % "1.2.1"
libraryDependencies += "org.scala-lang" % "jline" % "2.11.0-M3"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
libraryDependencies += "junit" % "junit" % "4.8" % "test"
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test->default"

import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
    "scala" -> Apache2_0("2016", "Anton Wierenga"),
    "conf" -> Apache2_0("2016", "Anton Wierenga", "#")
)

enablePlugins(AutomateHeaderPlugin) 
enablePlugins(JavaAppPackaging)

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

scalacOptions += "-target:jvm-1.7"

parallelExecution in Test := false
