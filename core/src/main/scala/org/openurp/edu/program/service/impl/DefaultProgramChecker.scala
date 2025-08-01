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

package org.openurp.edu.program.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.EntityDao
import org.openurp.edu.program.model.{MajorPlan, Program, ProgramDoc}
import org.openurp.edu.program.service.{DocChecker, PlanChecker, ProgramChecker}

import scala.collection.mutable

class DefaultProgramChecker extends ProgramChecker {

  var entityDao: EntityDao = _

  var planCheckers: mutable.Buffer[PlanChecker] = Collections.newBuffer[PlanChecker]
  var docCheckers: mutable.Buffer[DocChecker] = Collections.newBuffer[DocChecker]

  def check(program: Program): Seq[String] = {
    val rs = Collections.newBuffer[String]
    val plan = entityDao.findBy(classOf[MajorPlan], "program", program)
    if (plan.isEmpty) {
      rs.addOne("缺少教学计划")
    } else {
      planCheckers foreach { pc =>
        if (pc.suitable(program)) {
          rs.addAll(pc.check(plan.head))
        }
      }
      if (docCheckers.nonEmpty) {
        val doc = entityDao.findBy(classOf[ProgramDoc], "program", program)
        if (doc.isEmpty) {
          rs.addOne("缺少方案文本内容")
        } else {
          docCheckers foreach { dc =>
            rs.addAll(dc.check(doc.head, plan.head))
          }
        }
      }
    }

    rs.toSeq
  }
}
