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
import org.openurp.base.edu.code.CourseType
import org.openurp.edu.grade.domain.{PlanAuditContext, PlanAuditListener}
import org.openurp.edu.grade.model.{CourseAuditResult, GroupAuditResult}
import org.openurp.edu.program.model.CourseGroup

/**
 * 按照课程类别匹配的审核监听器<br>
 * 精确按照课程代码匹配的审核逻辑场景中，不要添加该监听器.
 *
 * @since 2018.10.1
 */
class PlanAuditCourseTypeMatchListener extends PlanAuditListener {

  protected def addGroupResult(results: collection.mutable.Map[CourseType, GroupAuditResult], gr: GroupAuditResult) : Unit = {
    results.put(gr.courseType, gr)
    for (child <- gr.children) {
      addGroupResult(results, child)
    }
  }

  override def end(context: PlanAuditContext) : Unit = {
    val results = Collections.newMap[CourseType, GroupAuditResult]
    val stdGrade = context.stdGrade
    val restCourses = stdGrade.restCourses
    if (restCourses.nonEmpty) {
      val result = context.result
      for (gr <- result.groupResults) {
        addGroupResult(results, gr)
      }
    }
    for (course <- restCourses) {
      val grades = stdGrade.getGrades(course)
      var courseType: CourseType = null
      var groupResult: GroupAuditResult = null
      var g: CourseGroup = null
      // 没有成绩
      if (grades.nonEmpty) courseType = grades.head.courseType
      if (null != courseType) groupResult = results.get(courseType).orNull
      if (null == groupResult)
        g = context.coursePlan.getGroup(groupResult.courseType).orNull
      // 计划里的必修组，不能按照类别匹配
      if (null != groupResult && (null == g || g.children.isEmpty && g.courseType.optional)) {
        stdGrade.useGrades(course)
        val remark = new StringBuilder()
        /*
       * 判断是否为计划外课程，如果课程组不为空，那么剩余的课程都是计划外课程
       * 如果课程组为空，那么剩余的课程不算是计划外课程
       */
        val courseGroup = context.coursePlan.getGroup(courseType).orNull
        var outOfPlan = false
        if (!Collections.isEmpty(courseGroup.planCourses)) {
          outOfPlan = true
        }
        var existResult: CourseAuditResult = null
        var existed = false
        groupResult.courseResults.find(_.course == course) foreach { cr =>
          existResult = cr
          existed = true
        }
        if (existResult == null) existResult = new CourseAuditResult()
        existResult.course = course
        existResult.checkPassed(grades)
        groupResult.updateCourseResult(existResult)
        if (null != existResult.remark) remark.insert(0, existResult.remark)
        if (outOfPlan) remark.append(" 计划外")
        existResult.remark = Some(remark.toString)
        if (!existed) groupResult.addCourseResult(existResult)
        groupResult.checkPassed(true)
      }
    }
  }

  override def startGroup(context: PlanAuditContext, courseGroup: CourseGroup, groupResult: GroupAuditResult): Boolean = {
    true
  }

  override def start(context: PlanAuditContext): Boolean = true

}
