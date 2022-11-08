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

import org.beangle.commons.collection.Collections
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.BaseServiceImpl
import org.openurp.edu.grade.service.TranscriptDataProvider
import org.openurp.edu.program.domain.CoursePlanProvider
import org.openurp.edu.program.model.PlanCourse

class TranscriptPlanCourseProvider extends BaseServiceImpl with TranscriptDataProvider {

   var coursePlanProvider: CoursePlanProvider = _

  def dataName: String = "planCourses"

  def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef = {
    stds.map { std =>
      (std, getPlanCourses(std))
    }.toMap
  }

  private def getPlanCourses(std: Student): collection.Seq[PlanCourse] = {
    val planCourses = Collections.newBuffer[PlanCourse]
    val plan = coursePlanProvider.getCoursePlan(std).orNull
    if (plan == null) {
      for (courseGroup <- plan.groups)
        planCourses ++= courseGroup.planCourses
    }
    planCourses
  }

}
