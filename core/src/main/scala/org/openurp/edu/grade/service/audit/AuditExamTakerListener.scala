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

import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.code.edu.model.{ExamType, GradeType}
import org.openurp.edu.exam.model.ExamTaker
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.*

class AuditExamTakerListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    //查询没有出补缓考成绩的考试记录
    val builder = OqlBuilder.from(classOf[ExamTaker], "et").where("et.std=:std", context.std)
    builder.where("et.examType.id in(:makeupTypeIds)", Seq(ExamType.Makeup, ExamType.Delay))
    builder.where(s"not exists(from ${classOf[ExamGrade].getName} eg where " +
      " eg.courseGrade.clazz=et.clazz and eg.courseGrade.std=et.std" +
      s" and eg.gradeType.id in(${GradeType.Makeup},${GradeType.Delay}) and eg.status=:status)", Grade.Status.Published)
    val examTakers = entityDao.search(builder)

    if (examTakers.nonEmpty) {
      val examCourses = examTakers.map(_.clazz.course).distinct
      for (groupResult <- context.result.groupResults) {
        for (car <- groupResult.courseResults) {
          if (!car.passed && examCourses.contains(car.course)) {
            car.addRemark("未出补缓考成绩")
            car.taking = true
            groupResult.addCourseResult(car)
          }
        }
      }
    }
  }

}
