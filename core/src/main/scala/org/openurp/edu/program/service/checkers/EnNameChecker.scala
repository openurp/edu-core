/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openurp.edu.program.service.checkers

import org.beangle.commons.collection.Collections
import org.beangle.commons.json.{Json, JsonObject}
import org.beangle.commons.lang.Charsets
import org.beangle.commons.net.http.HttpUtils
import org.beangle.ems.app.Ems
import org.openurp.base.edu.model.CourseJournal

import java.net.URLEncoder

/** 课程英文名
 */
class EnNameChecker {

  def check(journals: Iterable[CourseJournal]): Map[CourseJournal, String] = {
    val result = Collections.newMap[CourseJournal, String]
    journals foreach { journal =>
      journal.enName match
        case None => result.put(journal, "缺少英文名")
        case Some(enName) =>
          val rs = Json.parse(HttpUtils.getText(Ems.api + "/tools/lang/en/check.json?name=" +
            URLEncoder.encode(enName, Charsets.UTF_8)).getText).asInstanceOf[JsonObject]
          if (!rs.getBoolean("success")) {
            result.put(journal, rs.getArray("data").map(_.toString).mkString(";"))
          }
    }
    result.toMap
  }
}
