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

package org.openurp.edu.ws.clazz

import org.beangle.commons.collection.Properties
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.ems.app.Ems
import org.beangle.web.action.annotation.{mapping, param, response}
import org.beangle.web.action.support.ActionSupport
import org.openurp.base.service.SemesterService
import org.openurp.base.std.model.Student
import org.openurp.edu.clazz.domain.ClazzProvider
import org.openurp.edu.clazz.model.{Clazz, ClazzNotice}

import java.time.LocalDate

class NoticeWS extends ActionSupport {

  var entityDao: EntityDao = _
  var semesterService: SemesterService = _
  var clazzProvider: ClazzProvider = _

  @response
  @mapping("student/{code}")
  def student(@param("code") code: String): Properties = {
    val stds = entityDao.findBy(classOf[Student], "code", code)
    val data = stds.headOption match {
      case None => List(new Properties())
      case Some(std) =>
        val semester = semesterService.get(std.project, LocalDate.now)
        val clazzes = clazzProvider.getClazzes(semester, std).map(_.clazz)
        val notices = getQueryBuilder(clazzes)
        notices.map { notice =>
          val prop = new Properties(notice, "id", "title", "contents", "updatedAt")
          prop.put("href", Ems.base + s"/edu/learning/notice/${notice.id}")
          val clazz = new Properties(notice.clazz, "id", "crn")
          val course = new Properties(notice.clazz.course, "id", "code", "name")
          clazz.put("course", course)
          prop.put("clazz", clazz)
          prop
        }
    }
    val rs = new Properties()
    rs.put("data", data)
    rs
  }

  private def getQueryBuilder(clazzes: collection.Seq[Clazz]): Seq[ClazzNotice] = {
    val query = OqlBuilder.from(classOf[ClazzNotice], "cn")
    query.where("cn.clazz in(:clazzes)", clazzes)
    query.orderBy("cn.updatedAt desc")
    entityDao.search(query)
  }
}
