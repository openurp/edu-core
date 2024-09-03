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

import org.beangle.data.dao.EntityDao
import org.openurp.code.edu.model.CourseType
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.{AuditCourseResult, AuditGroupResult}

/** 计划外课程
 */
class AuditLastListener extends AuditPlanListener{
  var entityDao: EntityDao = _

  override def end(context: AuditPlanContext): Unit = {
    val result = context.result
    val stdGrade = context.stdGrade
    val rests = stdGrade.restCourses
    val outofPlanTypes = entityDao.findBy(classOf[CourseType],"name","计划外")
    if(outofPlanTypes.nonEmpty && rests.nonEmpty) {
     val outofPlanType = outofPlanTypes.head
     val gr =
       result.groupResults.find(_.courseType == outofPlanType) match {
         case None=>
           val gr= new AuditGroupResult("99.99", outofPlanType.name, outofPlanType)
           result.addGroupResult(gr)
           gr
         case Some(gr)=> gr
       }

      gr.indexno = "99.99" //尽量放到最后
      for (course <- rests) {
        val grades = stdGrade.useGrade(course)
        val credits = course.getCredits(result.std.level)
        if (credits > 0) {
          val cr = gr.getCourseResult(course).getOrElse(new AuditCourseResult(course))
          grades foreach { grade =>
            cr.addRemark("原" + grade.courseType.name)
          }
          cr.updatePassed(grades)
          gr.addCourseResult(cr)
        }
      }
    }
  }
}
