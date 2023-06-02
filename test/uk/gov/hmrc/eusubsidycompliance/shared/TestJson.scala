/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliance.shared

import play.api.libs.json.{JsArray, JsBoolean, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue}

object TestJson {

  //Use ops to make it clear in the import that we are doing implicit stuff. Scala 3 has keywords to deal with this
  //This just makes it much more friendly on the fingers when writing tests (everything becomes .asJson). Plus easy to visually memorize stuff
  //when there is little line noise.
  object ops {

    implicit class JsonStringOps(string: String) {
      val asJson: JsString = JsString(string)
    }

    implicit class JsonBooleanOps(boolean: Boolean) {
      val asJson: JsBoolean = {
        if (boolean) {
          JsTrue
        } else {
          JsFalse
        }
      }
    }

    implicit class JsonValueOps(value: JsValue) {

      val asHumanString: String = value match {
        case JsNull => null
        case JsFalse => "false"
        case JsTrue => "true"
        case JsNumber(numberValue) => numberValue.toString
        case JsString(stringValue) => stringValue
        //We can either toString or raise errors for these, I think .toString is a better hint then error
        case JsArray(values) => values.toString
        case JsObject(underlying) => underlying.toString
      }

    }

  }

}
