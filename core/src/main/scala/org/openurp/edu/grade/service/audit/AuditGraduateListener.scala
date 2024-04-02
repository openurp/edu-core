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

import org.beangle.commons.lang.time.Weeks
import org.beangle.data.dao.EntityDao
import org.openurp.base.edu.model.Terms
import org.openurp.base.service.SemesterService
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}

import java.time.LocalDate

/** 针对毕业生的毕业课程的预审
 */
class AuditGraduateListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  var semesterService: SemesterService = _

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val plan = context.coursePlan
    val std = context.std
    val now = LocalDate.now
    //毕业学年
    val isGraduate = std.graduateOn.isBefore(now) || Weeks.between(now, std.graduateOn) <= 52

    if (!context.result.passed && isGraduate) {
      val plan = context.coursePlan
      //太短的学期不要看了，6~8学期以上在考虑最后两个学期作为毕业学年
      val terms = if plan.terms > 4 then Terms(s"${plan.terms - 1}-${plan.terms}") else Terms.empty

      for (groupResult <- context.result.groupResults) {
        for (car <- groupResult.courseResults) {
          if (!car.passed && (car.terms.matches(terms) || car.course.name.contains("毕业"))) {
            car.predicted = true
            car.addRemark("毕业学年课程")
            groupResult.addCourseResult(car)
          }
        }
      }
    }
  }
}
