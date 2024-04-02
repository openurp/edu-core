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

/**
 * 按照课程类别匹配的审核监听器<br>
 * 精确按照课程代码匹配的审核逻辑场景中，不要添加该监听器.
 * 处理剩余成绩按照类别放入计划的课程组，取决于选修组是否允许计划外课程
 * 匹配不上的，不要处理，留给公选课监听器
 *
 * @see AuditTypeMatchPolicy
 * @since 2018.10.1
 */
class AuditCourseTypeMatchListener extends AuditPlanListener {

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val stdGrade = context.stdGrade
    val restCourses = stdGrade.restCourses
    for (course <- restCourses) {
      val grades = stdGrade.getGrade(course)
      val courseType = grades.head.courseType
      context.getGroup(course, courseType) foreach { g =>
        val gr = context.result.getGroupResult(g.name).get //需要按照组的名称查找，可能这个组已经不是courseType类别的了
        // 计划里的必修组，不能按照类别匹配
        stdGrade.useGrade(course)
        val cr = gr.getCourseResult(course).getOrElse(new AuditCourseResult(course)).updatePassed(grades)
        //判断是否为计划外课程，如果课程组包含课程，那么剩余的课程都是计划外课程
        if (courseType != gr.courseType) cr.addRemark(s"原${courseType.name}")
        gr.addCourseResult(cr)
      }

    }
  }
}
