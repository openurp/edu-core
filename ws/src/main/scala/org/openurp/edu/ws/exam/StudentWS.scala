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

package org.openurp.edu.ws.exam

import org.beangle.commons.collection.Properties
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.web.action.annotation.{mapping, param, response}
import org.beangle.web.action.context.Params
import org.beangle.web.action.support.ActionSupport
import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.edu.exam.model.ExamTaker

class StudentWS extends ActionSupport {

  var entityDao: EntityDao = _

  @response
  @mapping("{stdId}/{semesterId}")
  def index(@param("stdId") stdId: String, @param("semesterId") semesterId: String): Properties = {
    getResult(entityDao.get(classOf[Student], stdId.toLong), entityDao.get(classOf[Semester], semesterId.toInt),
      Params.getInt("examTypeId"))
  }

  @response
  @mapping("")
  def index2(): Properties = {
    val props = new Properties
    props.put("student", "Invalid student code")
    props.put("semester", "Invalid semester code")
    var stds: Seq[Student] = null
    var semesters: Seq[Semester] = null
    Params.get("studentCode") foreach { stdCode =>
      stds = entityDao.findBy(classOf[Student], "user.code", List(stdCode))
    }
    Params.get("semesterCode") foreach { semesterCode =>
      semesters = entityDao.search(OqlBuilder.from(classOf[Semester], "s").where("s.code=:code", semesterCode).cacheable())
    }
    if (semesters.nonEmpty && stds.nonEmpty) {
      getResult(stds.head, semesters.head, Params.getInt("examTypeId"))
    } else {
      null
    }
  }

  private def getResult(std: Student, semester: Semester, examTypeId: Option[Int]): Properties = {
    val builder = OqlBuilder.from(classOf[ExamTaker], "es")
    builder.where("es.std=:std and es.semester=:semester", std, semester)
    examTypeId foreach { et =>
      builder.where("es.activity.examType.id=:examTypeId", et)
    }
    val ess = entityDao.search(builder)

    val rs = new Properties
    if (ess.nonEmpty) {
      val es = ess.head
      rs.put("semesterCode", es.semester.code)
      rs.put("studentName", es.std.name)
      rs.put("activities", ess.map(convert))
    }
    rs
  }

  private def convert(es: ExamTaker): Properties = {
    val props = new Properties(es, "examType.name", "examStatus.name")
    props.put("crn", es.clazz.crn)
    props.put("course", new Properties(es.clazz.course, "code", "name"))
    es.activity foreach { ea =>
      if (ea.publishState.timePublished) {
        props.put("examTime", ea.examOn.toString + " " + ea.beginAt.toString + "~" + ea.endAt.toString)
        props.put("seatNo", es.seatNo)
      }
      if (ea.publishState.roomPublished && es.examRoom.isDefined) {
        props.put("examRoom", es.examRoom.get.room.name)
      }
    }
    props
  }
}
