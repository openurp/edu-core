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
import org.openurp.edu.grade.domain.{PlanAuditContext, PlanAuditListener}
import org.openurp.edu.grade.model.{CourseAuditResult, GroupAuditResult, PlanAuditResult}
import org.openurp.edu.program.model.{CourseGroup, PlanCourse}

class PlanAuditCommonElectiveListener extends PlanAuditListener {

  def end(context: PlanAuditContext): Unit = {
    val result = context.result
    val stdGrade = context.stdGrade
    context.coursePlan.program.offsetType foreach { electiveType =>
      val groupResult =
        result.getGroupResult(electiveType) match {
          case Some(r) => r
          case None =>
            val groupRs = new GroupAuditResult()
            groupRs.courseType = electiveType
            groupRs.name = electiveType.name
            groupRs.subCount = 0.toShort
            groupRs.indexno = "99.99"
            result.addGroupResult(groupRs)
            groupRs
        }
      for (course <- stdGrade.restCourses) {
        val courseResult = new CourseAuditResult()
        courseResult.course = course
        val grades = stdGrade.useGrades(course)
        if (grades.nonEmpty && grades.head.courseType.id != electiveType.id) {
          courseResult.remark = Some("计划外")
        }
        courseResult.checkPassed(grades)
        groupResult.addCourseResult(courseResult)
      }
      processConvertCredits(groupResult, result, context)
      groupResult.checkPassed(true)
    }
  }

  protected def processConvertCredits(target: GroupAuditResult, result: PlanAuditResult, context: PlanAuditContext): Unit = {
    val parents = Collections.newSet[GroupAuditResult]
    val sibling = Collections.newSet[GroupAuditResult]
    var start = target.parent.orNull
    while (null != start && !parents.contains(start)) {
      parents.add(start)
      start = start.parent.orNull
    }
    target.parent foreach { parent =>
      sibling ++= parent.children
      sibling.remove(target)
    }
    var otherConverted = 0f
    var siblingConverted = 0f
    for (gr <- result.groupResults) {
      //      var skip = false
      //      skip = (!context.setting.isConvertable(gr.courseType))
      //      if (!skip) {
      //        skip = (gr == target || parents.contains(gr))
      //      }
      //      if (!skip) {
      if (sibling.contains(gr)) {
        siblingConverted += (if (gr.passed) gr.auditStat.passedCredits - gr.auditStat.requiredCredits else 0f)
      } else if (gr.parent.isEmpty) {
        otherConverted += (if (gr.passed) gr.auditStat.passedCredits - gr.auditStat.requiredCredits else 0f)
      }
      //      }
    }
    target.auditStat.convertedCredits = (otherConverted + siblingConverted)
    for (r <- parents) r.auditStat.convertedCredits = otherConverted
  }

  override def start(context: PlanAuditContext): Boolean = true

  def startCourseAudit(context: PlanAuditContext, groupResult: GroupAuditResult,
                       planCourse: PlanCourse): Boolean = {
    true
  }

  override def startGroup(context: PlanAuditContext, courseGroup: CourseGroup,
                          groupResult: GroupAuditResult): Boolean = {
    true
  }
}
