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

package org.openurp.edu.openapi.grade

import org.beangle.commons.collection.Properties
import org.beangle.commons.json.{Json, JsonArray, JsonObject}
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.data.json.JsonAPI
import org.beangle.webmvc.annotation.{mapping, response}
import org.beangle.webmvc.context.ActionContext
import org.beangle.webmvc.support.{ActionSupport, MimeSupport}
import org.openurp.base.edu.model.Course
import org.openurp.base.model.{Semester, User}
import org.openurp.code.edu.model.CourseType
import org.openurp.edu.grade.model.{CourseGrade, Grade}
import org.openurp.edu.openapi.AppVerifyInterceptor

/** 查询学生课程成绩
 */
class StdWS extends ActionSupport, MimeSupport {

  var entityDao: EntityDao = _

  @response
  @mapping("user/{usercode}")
  def user(usercode: String): Json = {
    val user = entityDao.findBy(classOf[User], "code" -> usercode).headOption
    if (user.isEmpty) {
      val rs = new JsonObject()
      rs.add("code", 200)
      rs.add("data", new JsonArray())
      rs
    } else {
      val q = OqlBuilder.from(classOf[CourseGrade], "cg")
      import q.given
      q.where { cg =>
        cg.std.user.equal(user.get)
          .and(cg.status equal Grade.Status.Published)
      }
      val grades = entityDao.search(q)

      if (isAcceptJsonApi) {
        val context = JsonAPI.context(ActionContext.current.params)
        context.filters.include(classOf[Semester], "id", "code", "name", "schoolYear")
        context.filters.include(classOf[Course], "id", "code", "name", "creditHours", "defaultCredits")
        context.filters.include(classOf[CourseType], "id", "code", "name")
        context.mkJson(grades, "id", "crn", "course", "courseType", "semester", "passed", "scoreText", "gp", "credits")
      } else {
        val props = grades.map { grade =>
          val course = new Properties(grade.course, "id", "code", "name", "creditHours")
          val semester = new Properties(grade.semester, "id", "code", "schoolYear", "name")
          val courseType = new Properties(grade.courseType, "id", "name")
          val gradingMode = new Properties(grade.gradingMode, "id", "name")
          val g = new Properties(grade, "id", "gp", "credits", "scoreText", "passed", "crn")
          g.put("semester", semester)
          g.put("course", course)
          g.put("courseType", courseType)
          g.put("gradingMode", gradingMode)
          g
        }

        val rs = new JsonObject()
        rs.add("code", 200)
        rs.add("data", new JsonArray(props))
        rs
      }
    }
  }

}
