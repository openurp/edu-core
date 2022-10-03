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
import org.openurp.base.edu.model.Course
import org.openurp.edu.grade.domain.GradeComparator
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.model.CourseAuditResult
import org.openurp.edu.program.domain.AlternativeCourseProvider
import org.openurp.edu.program.model.{AlternativeCourse, CourseGroup, PlanCourse}

object PlanAuditCourseSubstitutionListener {

  val substitutions_str = "substitutions"
}

import org.openurp.edu.grade.domain.{PlanAuditContext, PlanAuditListener, StdGrade}
import org.openurp.edu.grade.model.GroupAuditResult
import org.openurp.edu.grade.service.impl.PlanAuditCourseSubstitutionListener._

class PlanAuditCourseSubstitutionListener extends PlanAuditListener {

  var alternativeCourseProvider: AlternativeCourseProvider = _

  private def groupKey(courseGroup: CourseGroup): String = {
    courseGroup.name + "c"
  }

  override def start(context: PlanAuditContext): Boolean = {
    context.params.put(substitutions_str, alternativeCourseProvider.getAlternatives(context.result.std))
    true
  }

  override def end(context: PlanAuditContext) : Unit = {
  }

  def startCourseAudit(context: PlanAuditContext, groupResult: GroupAuditResult, planCourse: PlanCourse): Boolean = {
    val substituted = context.params(groupKey(planCourse.group)).asInstanceOf[Set[Course]]
    !(substituted.contains(planCourse.course))
  }

  def startGroup(context: PlanAuditContext, courseGroup: CourseGroup, groupResult: GroupAuditResult): Boolean = {
    val substituted = context.params.getOrElseUpdate(groupKey(courseGroup), Collections.newSet[Course])
      .asInstanceOf[collection.mutable.Set[Course]]
    val substitutions = context.params(substitutions_str).asInstanceOf[collection.Seq[AlternativeCourse]]
    val stdGrade = context.stdGrade
    val courseMap = Collections.newMap[Course, PlanCourse]
    for (planCourse <- courseGroup.planCourses) {
      courseMap.put(planCourse.course, planCourse)
    }
    for (sc <- substitutions if sc.olds.subsetOf(courseMap.keySet) && isSubstitutes(stdGrade, sc)) {
      val substituteGrades = Collections.newBuffer[CourseGrade]
      for (c <- sc.news) {
        substituteGrades ++= stdGrade.getGrades(c)
        stdGrade.addNoGradeCourse(c)
      }
      for (ori <- sc.olds) {
        val planCourse = courseMap.get(ori).asInstanceOf[PlanCourse]
        val planCourseResult = new CourseAuditResult(planCourse)
        planCourseResult.checkPassed(stdGrade.getGrades(ori), substituteGrades)
        val tempStr = new StringBuffer()
        substituteGrades foreach { grade =>
          tempStr.append(grade.course.name).append('[')
            .append(grade.course.code)
            .append("],")
        }
        if (tempStr.length > 0) tempStr.deleteCharAt(tempStr.length - 1)
        planCourseResult.remark = Some(tempStr.toString)
        groupResult.addCourseResult(planCourseResult)
        groupResult.checkPassed(false)
        substituted += ori
      }
    }
    true
  }

  protected def isSubstitutes(stdGrade: StdGrade, ac: AlternativeCourse): Boolean = {
    val allCourses = Collections.newSet[Course]
    allCourses ++= ac.olds
    allCourses ++= ac.news

    val subGrades = Collections.newMap[Course, CourseGrade]
    for (course <- allCourses) {
      val grades = stdGrade.getGrades(course)
      if (Collections.isNotEmpty(grades)) subGrades.put(course, grades.head)
    }
    if (GradeComparator.isSubstitute(ac, subGrades)) {
      for (course <- allCourses) stdGrade.useGrades(course)
      true
    } else {
      false
    }
  }
}
