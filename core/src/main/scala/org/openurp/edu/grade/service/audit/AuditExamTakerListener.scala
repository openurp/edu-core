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
import org.beangle.commons.lang.time.Weeks
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.base.model.Semester
import org.openurp.code.edu.model.{ExamType, GradeType}
import org.openurp.edu.exam.model.ExamTaker
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.*

import java.time.LocalDate

class AuditExamTakerListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    //查询没有出补缓考成绩的考试记录
    val builder = OqlBuilder.from(classOf[ExamTaker], "et").where("et.std=:std", context.std)
    builder.where("et.examType.id in(:makeupTypeIds)", Seq(ExamType.Makeup, ExamType.Delay))
    builder.where(s"not exists(from ${classOf[ExamGrade].getName} eg where " +
      " eg.courseGrade.clazz=et.clazz and eg.courseGrade.std=et.std" +
      s" and eg.gradeType.id in(${GradeType.Makeup},${GradeType.Delay}))")
    val examTakers = entityDao.search(builder)

    if (examTakers.nonEmpty) {
      val today = LocalDate.now
      //半年以内的未出成绩的补缓考
      val examCourses = Collections.newMap[Course, Semester]
      examTakers.filter(x => Math.abs(Weeks.between(x.semester.endOn, today)) <= 25) foreach { taker =>
        examCourses.getOrElseUpdate(taker.clazz.course, taker.semester)
      }
      for (groupResult <- context.result.groupResults) {
        for (car <- groupResult.courseResults) {
          if (!car.passed && examCourses.keySet.contains(car.course)) {
            val semester = examCourses(car.course)
            car.addRemark(s"未出补缓考成绩(${semester.schoolYear}学年${semester.name}学期)")
            car.taking = true
            groupResult.addCourseResult(car)
          }
        }
      }
    }
  }

}
