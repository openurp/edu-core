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

package org.openurp.edu.grade.service.impl.filters

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Course
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.grade.domain.GradeFilter
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.AlternativeCourse

import java.time.LocalDate

/** 初次成绩过滤器
 *
 * @param alternativeCourses
 */
class FirstGradeFilter(alternativeCourses: collection.Seq[AlternativeCourse]) extends GradeFilter {

  private def findRemoveGrades(gradeDates:collection.Map[Course,LocalDate],olds:collection.Set[Course],
                               news:collection.Set[Course],grades: Iterable[CourseGrade]):Iterable[CourseGrade]={
    if (olds.forall(x => gradeDates.contains(x))) {
      val minAcquiredOn = getMinAccquiredOn(gradeDates, olds).get
      getMinAccquiredOn(gradeDates, news) match {
        case None=> None
        case Some(newMinAcquiredOn) =>
          if newMinAcquiredOn.isBefore(minAcquiredOn) then None else grades.filter(x => news.contains(x.course))
      }
    }else{
      None
    }
  }

  override def filter(grades: Iterable[CourseGrade]): Iterable[CourseGrade] = {
    val gradeCourses = grades.groupBy(_.course).map(x=> (x._1, x._2.map(_.semester.beginOn).min))
    val removed = Collections.newSet[CourseGrade]

    alternativeCourses foreach { alt =>
      removed.addAll(findRemoveGrades(gradeCourses, alt.olds, alt.news, grades))
      removed.addAll(findRemoveGrades(gradeCourses, alt.news, alt.olds, grades))
    }

    val rs = Collections.newBuffer[CourseGrade]
    val endGaType = new GradeType(GradeType.EndGa)
    val delayGaType = new GradeType(GradeType.DelayGa)

    grades.foreach { grade =>
      if (!removed.contains(grade)) {
        val delayGa = grade.getGaGrade(delayGaType)
        val endGa = grade.getGaGrade(endGaType)
        val finalGa = delayGa match
          case None => endGa
          case Some(ga) => if ga.published then delayGa else endGa

        finalGa match
          case None => rs.addOne(grade)
          case Some(ga) =>
            if (ga.scoreText != grade.scoreText) {
              val g = clone(grade)
              g.gp = ga.gp
              g.score = ga.score
              g.scoreText = ga.scoreText
              g.passed = ga.passed
              rs.addOne(g)
            } else {
              rs.addOne(grade)
            }
      }
    }
    rs
  }

  private def getMinAccquiredOn(gradeCourses: collection.Map[Course, LocalDate], courses: collection.Set[Course]): Option[LocalDate] = {
    var minAccquiredOn: LocalDate = null
    for (course <- courses) {
      val d = gradeCourses.get(course).orNull
      if (null != d && (null == minAccquiredOn || d.isBefore(minAccquiredOn))) minAccquiredOn = d
    }
    Option(minAccquiredOn)
  }

  private def clone(ag: CourseGrade): CourseGrade = {
    val g = new CourseGrade
    g.project = ag.project
    g.id = ag.id
    g.semester = ag.semester
    g.course = ag.course
    g.courseTakeType = ag.courseTakeType
    g.courseType = ag.courseType
    g.status = ag.status
    g.std = ag.std
    g.score = ag.score
    g.scoreText = ag.scoreText
    g.passed = ag.passed
    g.gp = ag.gp
    g.gradingMode = ag.gradingMode
    g.clazz = ag.clazz
    g.crn = ag.crn
    g.freeListening = ag.freeListening
    g.operator = ag.operator
    g.createdAt = ag.createdAt
    g.updatedAt = ag.updatedAt
    g.examMode = ag.examMode
    g
  }
}
