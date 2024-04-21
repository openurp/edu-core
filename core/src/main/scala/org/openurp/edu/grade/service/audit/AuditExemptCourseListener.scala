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
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.code.edu.model.CourseType
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.{AuditGroupResult, CoursePassedWay}
import org.openurp.edu.program.model.{CourseGroup, ExemptCourse, StdExemptCourse}

class AuditExemptCourseListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  override def start(context: AuditPlanContext): Unit = {
    val std = context.result.std
    val query = OqlBuilder.from(classOf[ExemptCourse], "ec")
    query.where("ec.project=:project and ec.level=:level", std.project, std.level)
    query.where("ec.fromGrade.code <= :gradeCode", std.grade.code)
    query.cacheable()
    val ecs = entityDao.search(query)
    val exemptCourseTypes = Collections.newSet[CourseType]
    val exemptCourses = Collections.newSet[Course]
    for (ec <- ecs) {
      if (ec.toGrade.isEmpty || ec.toGrade.get.code.compareTo(std.grade.code) >= 0) {
        if (ec.stdTypes.contains(std.stdType)) {
          ec.course match
            case None => exemptCourseTypes.add(ec.courseType)
            case Some(c) => exemptCourses.add(c)
        }
      }
    }
    //补充个人免修课程
    exemptCourses ++= entityDao.findBy(classOf[StdExemptCourse], "std", std).map(_.course)

    context.params.put("exemptCourseTypes", exemptCourseTypes.toSet)
    context.params.put("exemptCourses", exemptCourses.toSet)
  }

  override def startGroup(context: AuditPlanContext, courseGroup: CourseGroup, groupResult: AuditGroupResult): Boolean = {
    val courseTypes = context.getParam("exemptCourseTypes", classOf[Set[CourseType]])
    !courseTypes.contains(courseGroup.courseType)
  }

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val std = context.result.std
    val exemptCourses = context.getParam("exemptCourses", classOf[Set[Course]])
    //如果没有免修课程，那就算了
    if (exemptCourses.isEmpty) return

    for (groupResult <- context.result.groupResults) {
      if (!groupResult.passed) {
        for (car <- groupResult.courseResults) {
          if (!car.passed && exemptCourses.contains(car.course)) {
            car.scores = "免修"
            car.passed = true
            car.passedWay = Some(CoursePassedWay.ByExemption)
            groupResult.addCourseResult(car)
          }
        }
      }
    }
  }
}
