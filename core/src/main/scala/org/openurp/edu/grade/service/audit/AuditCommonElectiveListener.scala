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

import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.AuditCourseResult

/** 任意选修课监听
 * 将其他模块多出的课程和学分，转换到任意选修课
 */
class AuditCommonElectiveListener extends AuditPlanListener {

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val result = context.result
    val stdGrade = context.stdGrade
    context.coursePlan.program.offsetType foreach { electiveType =>
      result.getGroupResult(electiveType.name) foreach { gr =>
        gr.indexno = "99.99" //尽量放到最后
        for (course <- stdGrade.restCourses) {
          val grades = stdGrade.useGrade(course)
          val credits = course.getCredits(result.std.level)
          if (credits > 0) {
            val cr = gr.getCourseResult(course).getOrElse(new AuditCourseResult(course))
            grades foreach { grade =>
              if (grade.courseType.id != electiveType.id) cr.addRemark("原" + grade.courseType.name)
            }
            cr.updatePassed(grades)
            gr.addCourseResult(cr)
          }
        }
      }
    }
  }
}
