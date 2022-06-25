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

package org.openurp.edu.ws.std

import org.beangle.commons.bean.Properties
import org.beangle.commons.lang.Strings
import org.beangle.commons.lang.reflect.BeanInfos
import org.beangle.commons.lang.reflect.TypeInfo.IterableType
import org.beangle.data.dao.EntityDao
import org.beangle.data.model.Entity
import org.beangle.security.Securities
import org.beangle.web.action.annotation.response
import org.beangle.web.action.context.{ActionContext, Params}
import org.beangle.web.action.support.ActionSupport
import org.openurp.base.edu.model.Course
import org.openurp.base.model.{Project, Semester}
import org.openurp.base.std.model.Student
import org.openurp.code.CodeBean
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.ws.helper.JsonAPI

class GradeWS extends ActionSupport {
  var entityDao: EntityDao = _
  var courseGradeProvider: CourseGradeProvider = _

  @response
  def index(): JsonAPI.Json = {
    val stds = entityDao.findBy(classOf[Student], "user.code", Securities.user)
    val grades = courseGradeProvider.getPublished(stds.head)
    val context = JsonAPI.context(ActionContext.current.params)
    context.filters.exclude(classOf[Any], "createdAt", "updatedAt","operator")
    context.filters.include(classOf[CodeBean], "code", "name", "enName")

    context.filters.exclude(classOf[CourseGrade], "clazz", "std", "status")
    context.filters.include(classOf[Project], "code", "name")
    context.filters.include(classOf[Course], "code", "name", "enName", "credits")
    context.filters.include(classOf[Semester], "code", "schoolYear", "name", "beginOn", "endOn")
    val resources = grades.map { g => JsonAPI.create(g, context, "") }
    JsonAPI.newJson(resources, context)
  }
}
