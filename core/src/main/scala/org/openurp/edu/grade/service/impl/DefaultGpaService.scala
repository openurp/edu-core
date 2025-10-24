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

import org.beangle.data.dao.EntityDao
import org.openurp.base.model.Semester
import org.openurp.base.service.ProjectConfigService
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.{CourseGradeProvider, GpaCalculator, GradeFilter, GradeFilters}
import org.openurp.edu.grade.model.{CourseGrade, StdGpa, StdSemesterGpa, StdYearGpa}
import org.openurp.edu.grade.service.GpaService

/** 缺省绩点计算服务
 */
class DefaultGpaService extends GpaService {

  private val calculator = new GpaCalculator()

  var courseGradeProvider: CourseGradeProvider = _

  var gradeFilterRegistry: GradeFilterRegistry = _

  var projectConfigService: ProjectConfigService = _

  var entityDao: EntityDao = _

  override def getGpa(std: Student): BigDecimal = {
    calculator.calcGpa(filter(courseGradeProvider.get(std)))
  }

  override def getGpa(std: Student, grades: collection.Iterable[CourseGrade]): BigDecimal = {
    calculator.calcGpa(filter(grades))
  }

  override def getGpa(std: Student, semester: Semester): BigDecimal = {
    calculator.calcGpa(filter(courseGradeProvider.get(std, List(semester))))
  }

  override def refresh(stdGpa: StdGpa): Unit = {
    val newer = stat(stdGpa.std, courseGradeProvider.get(stdGpa.std))
    merge(stdGpa, newer)
    entityDao.saveOrUpdate(stdGpa)
  }

  override def get(std: Student): StdGpa = {
    val exists = entityDao.findBy(classOf[StdGpa], "std", std).headOption
    exists match {
      case None =>
        val rs = stat(std, courseGradeProvider.get(std))
        entityDao.saveOrUpdate(rs)
        rs
      case Some(t) =>
        t
    }
  }

  override def stat(std: Student): StdGpa = {
    val rs = stat(std, courseGradeProvider.get(std))

    val exists = entityDao.findBy(classOf[StdGpa], "std", std).headOption
    exists match {
      case None =>
        entityDao.saveOrUpdate(rs)
        rs
      case Some(t) =>
        merge(t, rs)
        entityDao.saveOrUpdate(t)
        t
    }
  }

  protected def filter(grades: Iterable[CourseGrade]): Iterable[CourseGrade] = {
    if (grades.isEmpty) {
      grades
    } else {
      val std = grades.head.std
      val filterNames = projectConfigService.get(std.project, "edu.grade.gpa_filters", "")
      //添加非免修,和不计算绩点的内置规则
      var filters = gradeFilterRegistry.getFilters(filterNames).toList
      filters = GradeFilters.CalcGP :: GradeFilters.NotExemption :: filters
      val filter = GradeFilters.chain(filters: _*)
      filter.filter(grades)
    }
  }

  override def stat(std: Student, grades: collection.Seq[CourseGrade]): StdGpa = {
    val filterGrades = filter(grades)
    val stdGpa = calculator.calc(std, filterGrades, true)
    val stdGpa2 = calculator.calc(std, filterGrades, false)
    stdGpa.project = std.project
    stdGpa.gradeCount = stdGpa2.gradeCount
    stdGpa.credits = stdGpa2.credits
    stdGpa.totalCredits = stdGpa2.totalCredits
    stdGpa.wms = stdGpa2.wms
    stdGpa.ams = stdGpa2.ams
    stdGpa.gpa = stdGpa2.gpa
    stdGpa
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
    stat(std, courseGradeProvider.get(std, semesters))
  }

  private def merge(target: StdGpa, source: StdGpa): Unit = {
    target.wms = source.wms
    target.ams = source.ams
    target.gpa = source.gpa
    target.project = source.project
    target.gradeCount = source.gradeCount
    target.credits = source.credits
    target.totalCredits = source.totalCredits
    val existedTerms = target.semesterGpas.map(x => (x.semester, x)).toMap
    val sourceTerms = source.semesterGpas.map(x => (x.semester, x)).toMap
    sourceTerms foreach { (key, s) =>
      existedTerms.get(key) match {
        case None =>
          target.add(s)
        case Some(t) =>
          t.wms = s.wms
          t.ams = s.ams
          t.gpa = s.gpa
          t.gradeCount = s.gradeCount
          t.credits = s.credits
          t.totalCredits = s.totalCredits
      }
    }
    for ((key, value) <- existedTerms if !sourceTerms.contains(key)) {
      val targetTerm = value
      targetTerm.stdGpa = null
      target.semesterGpas -= targetTerm
    }
    val existedYears = target.yearGpas.map(x => (x.schoolYear, x)).toMap
    val sourceYears = source.yearGpas.map(x => (x.schoolYear, x)).toMap
    sourceYears foreach { (key, s) =>
      existedYears.get(key) match {
        case None =>
          target.add(s)
        case Some(t) =>
          t.wms = s.wms
          t.ams = s.ams
          t.gpa = s.gpa
          t.gradeCount = s.gradeCount
          t.credits = s.credits
          t.totalCredits = s.totalCredits
      }
    }
    for ((key, value) <- existedYears if !sourceYears.contains(key)) {
      val targetTerm = value
      targetTerm.stdGpa = null
      target.yearGpas -= targetTerm
    }
    target.updatedAt = source.updatedAt
  }

}
