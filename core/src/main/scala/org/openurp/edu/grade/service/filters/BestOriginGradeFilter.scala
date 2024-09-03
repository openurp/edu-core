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

package org.openurp.edu.grade.service.filters

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Course
import org.openurp.edu.grade.domain.{GradeComparator, GradeFilter}
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.domain.AlternativeCourseProvider
import org.openurp.edu.program.model.AlternativeCourse

class BestOriginGradeFilter extends GradeFilter {

  var alternativeCourseProvider: AlternativeCourseProvider = _

  private def buildGradeMap(grades: Iterable[CourseGrade]): collection.mutable.Map[Course, CourseGrade] = {
    val gradesMap = Collections.newMap[Course, CourseGrade]
    var old: CourseGrade = null
    for (grade <- grades) {
      old = gradesMap.get(grade.course).orNull
      if (GradeComparator.betterThan(grade, old)) {
        if (null != old) {
          var cloned = grade
          if (grade.semester.beginOn.isAfter(old.semester.beginOn)) {
            cloned = clone(grade)
            cloned.semester = old.semester
          }
          gradesMap.put(grade.course, cloned)
        } else {
          gradesMap.put(grade.course, grade)
        }
      }
    }
    gradesMap
  }

  private def clone(grade: CourseGrade): CourseGrade = {
    val cloned = new CourseGrade()
    cloned.std = grade.std
    cloned.course = grade.course
    cloned.semester = grade.semester
    cloned.clazz = grade.clazz
    cloned.crn = grade.crn
    cloned.courseType = grade.courseType
    cloned.courseTakeType = grade.courseTakeType
    cloned.freeListening = grade.freeListening
    cloned.examMode = grade.examMode
    cloned.gradingMode = grade.gradingMode
    cloned.project = grade.project
    cloned.gp = grade.gp
    cloned.passed = grade.passed
    cloned.score = grade.score
    cloned.scoreText = grade.scoreText
    cloned.status = grade.status
    cloned.examGrades ++= (grade.examGrades)
    cloned
  }

  override def filter(grades:Iterable[CourseGrade]): Iterable[CourseGrade] = {
    val gradesMap = buildGradeMap(grades)
    val substituteCourses = getAlternatives(grades)
    for (subCourse <- substituteCourses) {
      val origin = gradesMap.get(subCourse.olds.head).orNull
      val sub = gradesMap.get(subCourse.news.head).orNull
      if (null != origin && null != sub && GradeComparator.betterThan(sub, origin)) {
        gradesMap.remove(sub.course)
        val subClone = clone(sub)
        subClone.semester = origin.semester
        subClone.course = origin.course
        gradesMap.put(origin.course, subClone)
      }
    }
    gradesMap.values.toSeq
  }

  private def getAlternatives(grades: Iterable[CourseGrade]): collection.Seq[AlternativeCourse] = {
    if (grades.isEmpty) {
      List.empty
    } else {
      alternativeCourseProvider.getAlternatives(grades.head.std)
    }
  }

}
