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

package org.openurp.edu.ws.course

import org.beangle.data.dao.EntityDao
import org.beangle.webmvc.annotation.response
import org.beangle.webmvc.support.ActionSupport
import org.openurp.edu.course.flow.NewCourseApply
import org.openurp.edu.course.service.NewCourseService

class NewCourseWS extends ActionSupport {

  var entityDao: EntityDao = _

  var newCourseService: NewCourseService = _

  @response
  def check(): Seq[String] = {
    val apply = entityDao.get(classOf[NewCourseApply], getLongId("apply"))
    val result = newCourseService.check(apply)
    result
  }

  @response
  def generate(): String = {
    val apply = entityDao.get(classOf[NewCourseApply], getLongId("apply"))
    newCourseService.generate(apply)
  }

}
