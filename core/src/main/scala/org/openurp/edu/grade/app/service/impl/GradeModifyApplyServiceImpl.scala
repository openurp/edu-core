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

package org.openurp.edu.grade.app.service.impl

import org.beangle.data.dao.OqlBuilder
import org.openurp.edu.grade.app.model.GradeModifyApply
import org.openurp.edu.grade.app.service.GradeModifyApplyService
import org.openurp.edu.grade.model.{CourseGrade, ExamGrade}
import org.openurp.edu.grade.service.BaseServiceImpl

class GradeModifyApplyServiceImpl extends BaseServiceImpl with GradeModifyApplyService {

  def getCourseGrade(apply: GradeModifyApply): CourseGrade = {
    val builder = OqlBuilder.from(classOf[CourseGrade], "grade")
    builder.where("grade.semester  = :semester", apply.semester)
    builder.where("grade.project  = :project", apply.project)
    builder.where("grade.std  = :std", apply.std)
    builder.where("grade.course  = :course", apply.course)
    val grades = entityDao.search(builder)
    if (grades.isEmpty) null else grades.head
  }

  def getExamGrade(apply: GradeModifyApply): ExamGrade = {
    val builder = OqlBuilder.from(classOf[ExamGrade], "grade")
    builder.where("grade.courseGrade.semester  = :semester", apply.semester)
    builder.where("grade.courseGrade.project  = :project", apply.project)
    builder.where("grade.courseGrade.std  = :std", apply.std)
    builder.where("grade.courseGrade.course  = :course", apply.course)
    builder.where("grade.gradeType = :gradeType", apply.gradeType)
    val grades = entityDao.search(builder)
    if (grades.isEmpty) null else grades.head
  }
}
