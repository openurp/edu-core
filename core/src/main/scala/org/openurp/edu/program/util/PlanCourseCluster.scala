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

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.{Course, Terms}
import org.openurp.edu.program.model.{AbstractCourseGroup, CourseGroup, MajorPlanCourse, PlanCourse}

object PlanCourseCluster {

  def cluster(group: CourseGroup): Iterable[PlanCourse] = {
    val planCourses = group.orderedPlanCourses
    val clusters = planCourses.flatten(_.course.cluster).toSet
    if (clusters.isEmpty) {
      planCourses
    } else {
      val courseMap = planCourses.map(pc => (pc.course, pc)).toMap
      val courseSet = courseMap.keySet
      val filterClusters = clusters.filter { c => c.courses.forall(courseSet.contains) }

      if filterClusters.isEmpty then
        planCourses
      else
        val level = group.plan.program.level
        val grade = group.plan.program.grade
        val course2Clusters = Collections.newMap[Course, PlanCourse]
        filterClusters.map { c =>
          val fcs = courseMap.filter(x => c.courses.contains(x._1)).values
          val journals = fcs.map(_.course.getJournal(grade))
          val first = journals.head

          val tc = new Course() //temp course
          val codes = fcs.map(_.course.code).toSeq.sorted
          if (codes.size > 2) {
            tc.code = s"${codes.head}\n~\n${codes.last}"
          } else if (codes.size == 2) {
            tc.code = s"${codes.head}\n${codes.last}"
          } else {
            tc.code = codes.head
          }
          tc.name = c.name
          tc.nature = fcs.head.course.nature
          tc.enName = c.enName
          tc.defaultCredits = fcs.map(_.course.getCredits(level)).sum
          tc.examMode = first.examMode
          tc.department = first.department
          tc.creditHours = journals.map(_.creditHours).sum
          tc.weekHours = first.weekHours
          journals foreach { j =>
            j.hours foreach { jh =>
              tc.addHour(jh.nature, jh.creditHours)
            }
          }
          val npc = new MajorPlanCourse()
          npc.course = tc
          npc.terms = Terms(fcs.map(_.terms.toString).mkString(","))
          npc.group = group
          npc.remark = fcs.head.asInstanceOf[MajorPlanCourse].remark
          course2Clusters.put(first.course, npc)
        }
        val rs = Collections.newBuffer[PlanCourse]
        planCourses foreach { pc =>
          if (pc.course.cluster.isEmpty) {
            rs.addOne(pc)
          } else {
            if (filterClusters.contains(pc.course.cluster.get)) {
              rs.addAll(course2Clusters.get(pc.course))
            } else {
              rs.addOne(pc)
            }
          }
        }
        rs
    }
  }
}
