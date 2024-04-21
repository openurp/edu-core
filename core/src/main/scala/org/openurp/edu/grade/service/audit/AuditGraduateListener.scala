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
import org.openurp.base.service.SemesterService
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}

import java.time.LocalDate

/** 针对毕业生的毕业课程的预审
 */
class AuditGraduateListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  var semesterService: SemesterService = _

  var courseNames = Set("毕业实习", "毕业论文", "形势与政策", "社会调查与公益劳动", "专业见习", "学科竞赛、创新项目、模拟法庭等"
    , "模拟法庭等实践教学", "学年论文", "模拟法庭等实践教学", "军训")

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val plan = context.coursePlan
    val std = context.std
    val now = LocalDate.now
    //毕业学年
    val isGraduate = std.graduateOn.isBefore(now) || Weeks.between(now, std.graduateOn) <= 52

    if (!context.result.passed && isGraduate) {
      val plan = context.coursePlan
      for (groupResult <- context.result.groupResults) {
        for (car <- groupResult.courseResults) {
          if (!car.passed && (courseNames.contains(car.course.name) || car.course.name.startsWith("毕业论文"))) {
            car.predicted = true
            car.addRemark("毕业学年课程")
            groupResult.addCourseResult(car)
          }
        }
      }
    }
  }
}
