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

import org.beangle.cdi.bind.BindModule
import org.openurp.edu.grade.service.audit.*

class AuditModule extends BindModule {

  protected override def binding(): Unit = {
    //不要改变监听器的顺序
    bind("auditPlanListener.sameCourse", classOf[AuditSameCourseFilter])
    bind("auditPlanListener.courseAbility", classOf[AuditCourseAbilityListener])
    bind("auditPlanListener.alternative", classOf[AuditAlternativeListener])
    bind("auditPlanListener.exemptCourse", classOf[AuditExemptCourseListener])
    bind("auditPlanListener.courseTypeMatch", classOf[AuditCourseTypeMatchListener])
    bind("auditPlanListener.shareCourse", classOf[AuditShareCourseListener])
    bind("auditPlanListener.courseTaker", classOf[AuditCourseTakerListener])
    bind("auditPlanListener.examTaker", classOf[AuditExamTakerListener])
    bind("auditPlanListener.commonElective", classOf[AuditCommonElectiveListener])
    bind("auditPlanListener.graduate", classOf[AuditGraduateListener])

    //成绩中的类别优先，然后是公共课程，这样方便选修课类别替换
    bind("auditPlanService", classOf[AuditPlanServiceImpl]).property("defaultListenerNames",
      "courseAbility,alternative,exemptCourse,courseTypeMatch,shareCourse,courseTaker,examTaker,commonElective,graduate")
  }
}
