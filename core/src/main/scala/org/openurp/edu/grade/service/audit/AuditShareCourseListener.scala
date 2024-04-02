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

import org.beangle.data.dao.EntityDao
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.AuditCourseResult

/** 公共课程按照类别审核匹配
 * 处理剩余成绩放入对应类别的审核结果组,没有找到组的不处理，留给公选课监听器
 */
class AuditShareCourseListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    context.sharePlan.foreach { sharePlan =>
      val stdGrade = context.stdGrade
      val restCourses = stdGrade.restCourses
      if (restCourses.nonEmpty) {
        for (course <- restCourses; if context.shareCourses.contains(course)) {
          val grades = stdGrade.getGrade(course)
          val courseType = context.shareCourses(course)
          context.result.getGroupResult(courseType.name) foreach { gr =>
            val g = context.coursePlan.getGroup(gr.name).orNull
            stdGrade.useGrade(course)
            val cr = gr.getCourseResult(course).getOrElse(new AuditCourseResult(course)).updatePassed(grades)
            if (gr.courseType != grades.head.courseType) {
              cr.addRemark(s"原${grades.head.courseType.name}")
            }
            gr.addCourseResult(cr)
          }
        }
      }
    }
  }
}
