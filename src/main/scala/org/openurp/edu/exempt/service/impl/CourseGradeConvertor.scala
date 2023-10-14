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

package org.openurp.edu.exempt.service.impl

import org.beangle.commons.lang.Numbers
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.beangle.security.Securities
import org.openurp.base.edu.model.Course
import org.openurp.base.std.model.Student
import org.openurp.code.edu.model.{CourseTakeType, GradeType, GradingMode}
import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.exempt.service.impl.ExemptionCourse
import org.openurp.edu.grade.model.{CourseGrade, GaGrade, Grade}

import java.time.Instant

class CourseGradeConvertor(entityDao: EntityDao) {
  private val courseTakeType = new CourseTakeType()
  courseTakeType.id = CourseTakeType.Exemption
  private val gaGradeType = entityDao.get(classOf[GradeType], GradeType.EndGa)

  def convert(std: Student, ec: ExemptionCourse, provider: String, remark: String): CourseGrade = {
    val cgQuery = OqlBuilder.from(classOf[CourseGrade], "cg")
    cgQuery.where("cg.std=:std and cg.course=:course and cg.semester=:semester", std, ec.course, ec.semester)
    val courseGrades = entityDao.search(cgQuery)
    val courseGrade =
      if (courseGrades.isEmpty) {
        val cg = new CourseGrade
        cg.project = std.project
        cg.std = std
        cg.crn = "--"
        cg.provider = Some(provider)
        cg.semester = ec.semester
        cg.course = ec.course
        cg.createdAt = Instant.now
        cg.courseType = ec.courseType
        cg.examMode = ec.course.examMode
        cg
      } else {
        courseGrades.head
      }

    val ctQuery = OqlBuilder.from(classOf[CourseTaker], "ct")
    ctQuery.where("ct.std=:std and ct.course=:course and ct.semester=:semester", std, ec.course, ec.semester)
    entityDao.search(ctQuery) foreach { taker =>
      courseGrade.clazz = Some(taker.clazz)
      courseGrade.crn = taker.clazz.crn
      taker.takeType = courseTakeType
      taker.freeListening = true
      entityDao.saveOrUpdate(taker)
    }
    courseGrade.courseTakeType = courseTakeType
    courseGrade.gradingMode = ec.course.gradingModes.headOption.getOrElse(new GradingMode(GradingMode.Percent))
    courseGrade.freeListening = true
    courseGrade.passed = true
    courseGrade.status = Grade.Status.Published
    courseGrade.updatedAt = Instant.now
    courseGrade.operator = Some(Securities.user)
    courseGrade.updatedAt = Instant.now
    courseGrade.provider = Some(provider)
    courseGrade.remark = Some(remark)
    var gaGrade = courseGrade.getGaGrade(gaGradeType).orNull
    if (gaGrade == null) {
      gaGrade = new GaGrade()
      gaGrade.gradeType = gaGradeType
      gaGrade.createdAt = Instant.now
      courseGrade.addGaGrade(gaGrade)
    }
    gaGrade.gradingMode = courseGrade.gradingMode
    gaGrade.scoreText = courseGrade.scoreText
    gaGrade.score = courseGrade.score
    gaGrade.passed = true
    gaGrade.status = Grade.Status.Published
    gaGrade.gp = courseGrade.gp
    gaGrade.operator = Some(Securities.user)
    gaGrade.updatedAt = Instant.now
    courseGrade
  }

}
