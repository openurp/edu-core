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
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener, GradeComparator, StdGrade}
import org.openurp.edu.grade.model.{AuditCourseResult, CourseGrade}
import org.openurp.edu.program.domain.AlternativeCourseProvider
import org.openurp.edu.program.model.AlternativeCourse

class AuditAlternativeListener extends AuditPlanListener {

  var alternativeCourseProvider: AlternativeCourseProvider = _

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val substitutions = alternativeCourseProvider.getAlternatives(context.result.std)
    val stdGrade = context.stdGrade
    val courseMap = Collections.newMap[Course, AuditCourseResult]
    for (groupResult <- context.result.groupResults) {
      for (car <- groupResult.courseResults) {
        if !car.passed then courseMap.put(car.course, car)
      }
    }

    //如果有课程不及格，开始检查能否替代及格
    if (courseMap.nonEmpty) {
      for (sc <- substitutions if sc.olds.subsetOf(courseMap.keySet) && isSubstitutes(stdGrade, sc)) {
        val substituteGrades = Collections.newBuffer[CourseGrade]
        for (c <- sc.news) {
          substituteGrades ++= stdGrade.useGrade(c) //替代成绩不支持反复使用
        }
        // 增加原课程审核结果
        for (ori <- sc.olds) {
          val cr = courseMap(ori)
          cr.updatePassed(stdGrade.useGrade(cr.course), substituteGrades)
          cr.groupResult.addCourseResult(cr)
          if (cr.passed) courseMap.remove(ori) //有可能是一门不及格课程替代没有成绩的课程，所以判断以下是否还需保留
        }
      }
    }
  }

  protected def isSubstitutes(stdGrade: StdGrade, ac: AlternativeCourse): Boolean = {
    val allCourses = Collections.newSet[Course]
    allCourses ++= ac.olds
    allCourses ++= ac.news

    val subGrades = Collections.newMap[Course, CourseGrade]
    for (course <- allCourses) {
      stdGrade.getGrade(course) foreach { g => subGrades.put(course, g) }
    }
    GradeComparator.isSubstitute(ac, subGrades)
  }
}
