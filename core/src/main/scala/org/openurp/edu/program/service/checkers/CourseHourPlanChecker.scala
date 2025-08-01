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

/** 课程课时检查
 */
class CourseHourPlanChecker extends PlanChecker {
  override def check(plan: MajorPlan): Seq[String] = {
    val rs = Collections.newBuffer[String]
    for (g <- plan.groups; pc <- g.planCourses) {
      val journal = pc.journal
      if !journal.creditHourIdentical then
        rs.addOne(s"${pc.course.name} ${pc.course.defaultCredits}学分 总学时${journal.creditHours}和(理论+实践)不吻合")
    }
    rs.toSeq
  }

}
