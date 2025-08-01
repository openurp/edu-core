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

package org.openurp.edu.program.util

import org.openurp.edu.program.model.{CourseGroup, CoursePlan}

object PlanRender {

  def calcBranchLevel(plan: CoursePlan, maxCourseCntInLeaf: Int): Int = {
    val tops = plan.topGroups
    if tops.isEmpty then 0 else tops.map(g => calcBranchLevel(g, maxCourseCntInLeaf, 0)).max
  }

  private def calcBranchLevel(group: CourseGroup, maxCourseCntInLeaf: Int, fromLevel: Int): Int = {
    if group.children.isEmpty then
      if group.isLeaf || isOptionLeafGroup(group, maxCourseCntInLeaf) then fromLevel else fromLevel + 1
    else group.children.map(c => calcBranchLevel(c, maxCourseCntInLeaf, fromLevel + 1)).max
  }

  private def isOptionLeafGroup(group: CourseGroup, maxCourseCntInLeaf: Int): Boolean = {
    group.rank match
      case None => false
      case Some(r) => !r.compulsory && group.planCourses.size <= maxCourseCntInLeaf
  }
}
