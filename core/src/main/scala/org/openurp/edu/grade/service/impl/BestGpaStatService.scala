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

import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.{CourseGradeProvider, GpaPolicy}
import org.openurp.edu.grade.model.{CourseGrade, StdGpa, StdSemesterGpa, StdYearGpa}
import org.openurp.edu.grade.service.GpaStatService
import org.openurp.edu.grade.service.impl.filters.BestGradeFilter

class BestGpaStatService extends GpaStatService {

  var courseGradeProvider: CourseGradeProvider = _

  var gpaPolicy: GpaPolicy = _

  var bestGradeFilter: BestGradeFilter = _

  def refresh(stdGpa: StdGpa): Unit = {
    val newer = stat(stdGpa.std)
    merge(stdGpa, newer)
  }

  private def merge(target: StdGpa, source: StdGpa): Unit = {
    target.ga = source.ga
    target.gpa = source.gpa
    target.gradeCount = source.gradeCount
    target.credits = source.credits
    target.totalCredits = source.totalCredits
    val existedTerms = semesterGpa2Map(target.semesterGpas)
    val sourceTerms = semesterGpa2Map(source.semesterGpas)
    for ((key, value) <- sourceTerms) {
      val sourceTerm = value
      existedTerms.get(key) match {
        case None =>
          source.semesterGpas -= sourceTerm
          target.add(sourceTerm)
        case Some(targetTerm) =>
          targetTerm.ga = sourceTerm.ga
          targetTerm.gpa = sourceTerm.gpa
          targetTerm.gradeCount = sourceTerm.gradeCount
          targetTerm.credits = sourceTerm.credits
          targetTerm.totalCredits = sourceTerm.totalCredits
      }
    }
    for ((key, value) <- existedTerms if null == sourceTerms.get(key)) {
      val targetTerm = value
      targetTerm.stdGpa = null
      target.semesterGpas -= targetTerm
    }
    val existedYears = yearGpa2Map(target.yearGpas)
    val sourceYears = yearGpa2Map(source.yearGpas)
    for ((key, value) <- sourceYears) {
      val sourceTerm = value
      existedYears.get(key) match {
        case None =>
          source.yearGpas -= sourceTerm
          target.add(sourceTerm)
        case Some(targetTerm) =>
          targetTerm.ga = sourceTerm.ga
          targetTerm.gpa = sourceTerm.gpa
          targetTerm.gradeCount = sourceTerm.gradeCount
          targetTerm.credits = sourceTerm.credits
          targetTerm.totalCredits = sourceTerm.totalCredits
      }
    }
    for ((key, value) <- existedYears if null == sourceYears.get(key)) {
      val targetTerm = value
      targetTerm.stdGpa = null
      target.yearGpas -= targetTerm
    }
    target.updatedAt = source.updatedAt
  }

  private def semesterGpa2Map(gpas: Iterable[StdSemesterGpa]): Map[Semester, StdSemesterGpa] = {
    gpas.map { x => (x.semester, x) }.toMap
  }

  private def yearGpa2Map(gpas: Iterable[StdYearGpa]): Map[String, StdYearGpa] = {
    gpas.map { x => (x.schoolYear, x) }.toMap
  }

  def stat(std: Student): StdGpa = {
    stat(std, courseGradeProvider.getPublished(std))
  }

  override def stat(std: Student, grades: collection.Seq[CourseGrade]): StdGpa = {
    val stdGpa = gpaPolicy.calc(std, grades, true)
    val stdGpa2 = gpaPolicy.calc(std, bestGradeFilter.filter(grades), false)
    stdGpa.gradeCount = stdGpa2.gradeCount
    stdGpa.credits = stdGpa2.credits
    stdGpa.totalCredits = stdGpa2.totalCredits
    stdGpa.ga = stdGpa2.ga
    stdGpa.gpa = stdGpa2.gpa
    stdGpa
  }

  def refresh(stdGpa: StdGpa, grades: collection.Seq[CourseGrade]): Unit = {
    val newer = stat(stdGpa.std, grades)
    merge(stdGpa, newer)
  }

  override def stat(stds: Iterable[Student]): MultiStdGpa = {
    val multiStdGpa = new MultiStdGpa()
    for (std <- stds) {
      val stdGpa = stat(std)
      if (stdGpa != null) {
        multiStdGpa.stdGpas += stdGpa
      }
    }
    multiStdGpa.statSemestersFromStdGpa()
    multiStdGpa
  }

  override def statBySemester(stds: Iterable[Student], semesters: collection.Seq[Semester]): MultiStdGpa = {
    val multiStdGpa = new MultiStdGpa()
    for (std <- stds) {
      val stdGpa = statBySemester(std, semesters)
      if (stdGpa != null) {
        multiStdGpa.stdGpas += stdGpa
      }
    }
    multiStdGpa.statSemestersFromStdGpa()
    multiStdGpa
  }

  override def statBySemester(std: Student, semesters: collection.Seq[Semester]): StdGpa = {
    stat(std, courseGradeProvider.getPublished(std, semesters))
  }

}
