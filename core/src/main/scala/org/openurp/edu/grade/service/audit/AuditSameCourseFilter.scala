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

package org.openurp.edu.grade.service.audit

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Course
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.PlanCourse

class AuditSameCourseFilter extends AuditPlanListener {

  override def start(context: AuditPlanContext): Unit = {
    val plan = context.coursePlan
    //登记所有计划内课程
    val course2pc = Collections.newMap[Course, PlanCourse]
    for (g <- plan.groups; pc <- g.planCourses) {
      course2pc.put(pc.course, pc)
    }
    val stdGrade = context.stdGrade
    //所有成绩按照课程名称分组
    val courseNameMap = stdGrade.restCourses.map(stdGrade.getGrade(_).get).groupBy(_.course.name)
    courseNameMap foreach { case (courseName, grades) =>
      if (grades.size > 1) {
        val priorities = Collections.newBuffer[Tuple2[Int, CourseGrade]]
        grades foreach { g =>
          var priority = 0
          course2pc.get(g.course) foreach { pc =>
            //组内课程，选修优先级1，限选+1,必修组+10，必修+20
            if pc.group.courseType.optional then
              priority += 1
              if pc.group.courseType.name.contains("限") then //FIXME using courseRank
                priority += 1
              else priority += 10

            if (pc.compulsory) priority += 20
          }
          priorities.addOne(priority -> g)
          val sorted = priorities.sortBy(_._1).reverse
          val tail = sorted.tail
          tail foreach { g => stdGrade.useGrade(g._2.course) }
        }
      }
    }
  }

}
