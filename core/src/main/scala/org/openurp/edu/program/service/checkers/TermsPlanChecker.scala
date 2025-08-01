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
import org.openurp.edu.program.model.MajorPlan
import org.openurp.edu.program.service.PlanChecker

class TermsPlanChecker extends PlanChecker {
  override def check(plan: MajorPlan): Seq[String] = {
    val rs = Collections.newBuffer[String]
    val startTerm = plan.program.startTerm
    val endTerm = plan.program.endTerm
    for (g <- plan.groups; pc <- g.planCourses) {
      if (pc.terms == null) {
        rs.addOne(s"${pc.course.code} ${pc.course.name} [${g.name}] 没有设置开课学期")
      } else {
        val termList = pc.terms.termList
        if (termList.isEmpty) {
          rs.addOne(s"${pc.course.code} ${pc.course.name} [${g.name}] 没有设置开课学期")
        } else {
          val min = termList.min
          val max = termList.max
          if (min < startTerm) {
            rs.addOne(s"${pc.course.code} ${pc.course.name} [${g.name}] 的开课学期${min}过小")
          }
          if (max > endTerm) {
            rs.addOne(s"${pc.course.code} ${pc.course.name} [${g.name}] 的开课学期${max}过大")
          }
        }
      }
    }
    rs.toSeq
  }
}
