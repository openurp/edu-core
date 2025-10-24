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

package org.openurp.edu.grade.service.impl

import org.beangle.data.dao.OqlBuilder
import org.openurp.base.model.{Project, Semester}
import org.openurp.edu.grade.config.GradeInputSwitch
import org.openurp.edu.grade.service.{BaseServiceImpl, GradeInputSwitchService}

import java.time.Instant

class GradeInputSwithServiceImpl extends BaseServiceImpl with GradeInputSwitchService {

  def getSwitch(project: Project, semester: Semester): GradeInputSwitch = {
    val query = OqlBuilder.from(classOf[GradeInputSwitch], "switch")
    query.where("switch.project=:project", project)
    query.where("switch.semester=:semester", semester)
    entityDao.unique(query)
  }

  def getOpenedSemesters(project: Project): Seq[Semester] = {
    val query = OqlBuilder.from[Semester](classOf[GradeInputSwitch].getName, "s")
    query.where("s.project=:project", project)
    query.where("s.endAt>=:now", Instant.now)
    query.orderBy("s.semester.beginOn").select("s.semester")
    entityDao.search(query)
  }
}
