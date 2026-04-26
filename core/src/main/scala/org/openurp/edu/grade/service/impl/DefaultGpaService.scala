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
import org.openurp.edu.clazz.domain.CourseTakerProvider
import org.openurp.edu.grade.domain.{CourseGradeProvider, GpaCalculator, GradeFilter, GradeFilters}
import org.openurp.edu.grade.model.{CourseGrade, StdGpa, StdSemesterGpa, StdYearGpa}
import org.openurp.edu.grade.service.GpaService

/** 缺省绩点计算服务
 */
class DefaultGpaService extends GpaService {

  private val calculator = new GpaCalculator()

  var courseGradeProvider: CourseGradeProvider = _

  var courseTakerProvider: CourseTakerProvider = _

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
    val newer = stat(stdGpa.std)
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
    val grades = courseGradeProvider.get(std)

    //找出没有出成绩的修读记录
    val takers = courseTakerProvider.get(std)
    val semesterCourses = grades.map(x => (x.semester, x.course)).toSet
    val pendingTakers = takers.filter(t => !semesterCourses.contains((t.semester, t.course)))
    val rs = stat(std, grades)

    if (pendingTakers.nonEmpty) {
      val level = std.level
      pendingTakers.foreach { taker =>
        val credits = taker.course.getCredits(level)
        var s = rs.getSemesterGpa(taker.semester)
        if (null == s) {
          s = new StdSemesterGpa()
          s.semester = taker.semester
          rs.add(s)
        }
        var y = rs.getYearGpa(taker.semester.schoolYear)
        if (null == y) {
          y = new StdYearGpa
          y.schoolYear = taker.semester.schoolYear
          rs.add(y)
        }

        s.addPending(credits)
        y.addPending(credits)
        rs.addPending(credits)
      }
    }

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
      val chain = new GradeFilters.Chain(getFilters(grades))
      chain.filter(grades)
    }
  }

  protected def getFilters(grades: Iterable[CourseGrade]): Seq[GradeFilter] = {
    if (grades.isEmpty) {
      List.empty
    } else {
      val std = grades.head.std
      val filterNames = projectConfigService.get(std.project, "edu.grade.gpa_filters", "")
      //添加非免修,和不计算绩点的内置规则
      var filters = gradeFilterRegistry.getFilters(filterNames).toList
      filters = GradeFilters.defaults ::: filters
      filters
    }
  }

  override def stat(std: Student, grades: collection.Seq[CourseGrade]): StdGpa = {
    val filters = getFilters(grades)
    val stdGpa = calculator.calc(std, grades, true, filters)
    val stdGpa2 = calculator.calc(std, grades, false, filters)
    stdGpa.project = std.project
    stdGpa.totalCredits = stdGpa2.totalCredits
    stdGpa.totalCount = stdGpa2.totalCount
    stdGpa.credits = stdGpa2.credits
    stdGpa.takenCredits = stdGpa2.takenCredits
    stdGpa.pendingCredits = stdGpa2.pendingCredits

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
    target.totalCredits = source.totalCredits
    target.totalCount = source.totalCount
    target.credits = source.credits
    target.takenCredits = source.takenCredits
    target.pendingCredits = source.pendingCredits

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
          t.totalCredits = s.totalCredits
          t.totalCount = s.totalCount
          t.credits = s.credits
          t.takenCredits = s.takenCredits
          t.pendingCredits = s.pendingCredits
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
          t.totalCredits = s.totalCredits
          t.totalCount = s.totalCount
          t.credits = s.credits
          t.takenCredits = s.takenCredits
          t.pendingCredits = s.pendingCredits
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
