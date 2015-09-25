organization := "com.antonwierenga"

name := "activemq-cli"

version := "1.0.0"

scalaVersion := "2.11.6"

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
    "scala" -> Apache2_0("2015", "Anton Wierenga"),
    "conf" -> Apache2_0("2015", "Anton Wierenga", "#")
)

enablePlugins(AutomateHeaderPlugin) 

enablePlugins(JavaAppPackaging)