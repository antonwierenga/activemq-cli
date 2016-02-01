/*
 * Copyright 2015 Anton Wierenga
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

  val AnsiCodes = Map("Reset" → 0, "Black" → 30, "Red" → 31, "Green" → 32, "Yellow" → 33, "Blue" → 34, "Purple" → 35, "Cyan" → 36, "Gray" → 37)
  val AnsiEscapeCodeTemplate = "\u001B[%sm"

  def printInfo(text: String): Unit = {
    println(info(text)) // scalastyle:ignore
  }

  def prompt(text: String): String = decorate(text, AnsiCodes.get("Yellow").get)

  def info(text: String): String = decorate(text, AnsiCodes.get("Green").get)

  def warn(text: String): String = decorate(text, AnsiCodes.get("Purple").get)

  def decorate(text: String, ansiEscapeCode: Int): String = {
    AnsiEscapeCodeTemplate.format(ansiEscapeCode) + text + AnsiEscapeCodeTemplate.format(AnsiCodes.get("Reset").get)
  }
}
