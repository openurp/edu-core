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

package org.openurp.edu.grade.service

import org.beangle.commons.cdi.BindModule
import org.openurp.edu.grade.service.audit.*

class AuditModule extends BindModule {

  protected override def binding(): Unit = {
    //不要改变监听器的顺序
    bind("AuditPlanListener.sameCourse", classOf[AuditSameCourseFilter])
    bind("AuditPlanListener.courseAbility", classOf[AuditCourseAbilityListener])
    bind("AuditPlanListener.alternative", classOf[AuditAlternativeListener])
    bind("AuditPlanListener.exemptCourse", classOf[AuditExemptCourseListener])
    bind("AuditPlanListener.courseTypeMatch", classOf[AuditCourseTypeMatchListener])
    bind("AuditPlanListener.shareCourse", classOf[AuditShareCourseListener])
    bind("AuditPlanListener.courseTaker", classOf[AuditCourseTakerListener])
    bind("AuditPlanListener.examTaker", classOf[AuditExamTakerListener])
    bind("AuditPlanListener.commonElective", classOf[AuditCommonElectiveListener])
    bind("AuditPlanListener.graduate", classOf[AuditGraduateListener])
    bind("AuditPlanListener.last", classOf[AuditLastListener])

    //成绩中的类别优先，然后是公共课程，这样方便选修课类别替换
    bind("auditPlanService", classOf[AuditPlanServiceImpl]).property("defaultListenerNames",
      "courseAbility,alternative,exemptCourse,courseTypeMatch,shareCourse,courseTaker,examTaker,commonElective,graduate,last")
  }
}
