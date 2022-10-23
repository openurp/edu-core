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

package org.openurp.edu.schedule.service

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.time.{WeekDay, WeekTime}
import org.beangle.data.model.Entity
import org.openurp.base.edu.model.TimeSetting
import org.openurp.base.model.Semester
import org.openurp.edu.clazz.model.{Clazz, Session}

import scala.collection.mutable

object CourseTable {
  enum Style {
    /**
     * 1. WEEK_TABLE x-星期，y-小节
     * 2. UNIT_COLUMNx-小节，y-星期
     * 3. 逐次安排的列表
     */
    case WEEK_TABLE, UNIT_COLUMN, LIST
  }
}

class CourseTable(val semester: Semester, val resource: Object, val category: String) {
  var style = CourseTable.Style.WEEK_TABLE
  var clazzes: Seq[Clazz] = _
  var sessions: Seq[Session] = Seq.empty
  var placePublished: Boolean = _
  var timePublished: Boolean = _
  var timeSetting: TimeSetting = _

  def setClazzes(classList: Seq[Clazz]): Unit = {
    clazzes = classList
  }

  def setClazzes(classList: Seq[Clazz], wt: Iterable[WeekTime]): Unit = {
    clazzes = classList
    val ss = new mutable.ArrayBuffer[Session]
    if wt == null then
      sessions = clazzes.flatMap(_.schedule.sessions)
    else
      classList foreach { clazz =>
        clazz.schedule.sessions foreach { s =>
          val matched = wt.exists(wt => wt.startOn == s.time.startOn && (wt.weekstate & s.time.weekstate).value > 0)
          if matched then ss.addOne(s)
        }
      }
      sessions = ss.toSeq
  }

  def weekdays: collection.Seq[WeekDay] = {
    val days = Collections.newBuffer[WeekDay]
    var firstDay = semester.calendar.firstWeekday
    var i = 1
    while (i <= 7) {
      days += firstDay
      firstDay = firstDay.next
      i += 1
    }
    days
  }
}
