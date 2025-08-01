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

package org.openurp.edu.program.service

import org.beangle.commons.collection.Collections
import org.openurp.edu.program.model.*
import org.openurp.edu.program.service.PlanDiff.GroupDiff

trait CoursePlanService {

  def move(node: CourseGroup, location: CourseGroup, index: Int): Unit

  def statPlanCredits(plan: CoursePlan): Float

  def addCourseGroupToPlan(group: CourseGroup, parent: CourseGroup, plan: CoursePlan): Unit

  def addPlanCourse(planCourse: PlanCourse, group: CourseGroup): Unit

  def removePlanCourse(planCourse: PlanCourse, group: CourseGroup): Unit

  def updatePlanCourse(planCourse: PlanCourse, group: CourseGroup): Unit

  def diff(left: CoursePlan, right: CoursePlan): Seq[PlanDiff.GroupDiff]

  def getMajorPlans(programs: Iterable[Program]): Map[Program, MajorPlan]

  def generate(plan: MajorPlan): ExecutivePlan

  def copy(source: MajorPlan, target: MajorPlan): Unit
}

object PlanDiff {

  case class GroupDiff(indexno: String, name: String, commons: Seq[(PlanCourse, PlanCourse)], left: Option[Group], right: Option[Group]) {
    def diffCount: Int = {
      commons.size + Math.max(left.map(_.courses.size).getOrElse(0), right.map(_.courses.size).getOrElse(0))
    }
  }

  case class Group(credits: Float, courses: Seq[PlanCourse])
}

class PlanDiff {
  var groupDiffs = Collections.newBuffer[GroupDiff]
}
