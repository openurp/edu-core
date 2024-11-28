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

package org.openurp.edu.ws.schedule

import org.beangle.commons.collection.Properties
import org.beangle.commons.lang.Dates
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.webmvc.annotation.{mapping, param, response}
import org.beangle.webmvc.support.ActionSupport
import org.openurp.base.service.SemesterService
import org.openurp.base.std.model.Student
import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.schedule.service.LessonSchedule

class StudentWS extends ActionSupport {

  var entityDao: EntityDao = _
  var semesterService: SemesterService = _

  @response
  @mapping("{code}")
  def index(@param("code") code: String, @param("beginAt") beginAt: String, @param("endAt") endAt: String): Any = {
    val stds = entityDao.findBy(classOf[Student], "code", code)
    stds.headOption match
      case None => new Properties()
      case Some(std) =>
        val bAt = Dates.toDateTime(beginAt)
        val eAt = Dates.toDateTime(endAt)
        val semester = semesterService.get(std.project, bAt.toLocalDate)

        val takerQuery = OqlBuilder.from(classOf[CourseTaker], "ct")
        takerQuery.where("ct.std=:std and ct.semester=:semester", std, semester)
        val takers = entityDao.search(takerQuery)

        val activities = takers.flatMap(_.clazz.schedule.activities)
        val schedules = LessonSchedule.convert(activities, bAt, eAt)
        val properties = schedules.sortBy(_.orderWeekDayKey) map { schedule =>
          val properties = new Properties(schedule, "date", "time")
          val task = new Properties(schedule.task, "id", "semester", "subject", "taskType", "people", "crn")
          properties.put("task", task)
          properties
        }
        val rs = new Properties()
        rs.put("data", properties)
        rs
  }

}
