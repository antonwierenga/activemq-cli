/*
 * Copyright 2017 Anton Wierenga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package activemq.cli.util

object Console {

  val AnsiCodes = Map("reset" → "0", "gray" → "1;30", "light-red" → "1;31", "red" → "31", "light-green" → "1;32", "green" → "32", "light-yellow" → "1;33",
    "yellow" → "33", "light-blue" → "1;34", "blue" → "34", "light-purple" → "1;35", "purple" → "35", "light-cyan" → "1;36", "cyan" → "36",
    "light-white" → "1;37", "white" → "37")

  /*    37 white
    40 black background
    41 red background
    42 green background
    43 yellow background
    44 blue background
    45 magenta background
    46 cyan background
    47 white background*/

  val AnsiEscapeCodeTemplate = "\u001B[%sm"

  def printInfo(text: String): Unit = {
    println(info(text)) // scalastyle:ignore
  }

  def prompt(text: String): String = decorate(text, AnsiCodes.get("yellow").get)

  def info(text: String): String = decorate(text, AnsiCodes.get("green").get)

  def warn(text: String): String = decorate(text, AnsiCodes.get("purple").get)

  def decorate(text: String, ansiEscapeCode: String): String = {
    AnsiEscapeCodeTemplate.format(ansiEscapeCode) + text + AnsiEscapeCodeTemplate.format(AnsiCodes.get("reset").get)
  }
}
