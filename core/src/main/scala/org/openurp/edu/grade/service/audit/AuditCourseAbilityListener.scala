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
import org.beangle.data.dao.EntityDao
import org.openurp.edu.clazz.model.StdCourseAbility
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.{AuditCourseResult, AuditGroupResult}
import org.openurp.edu.program.domain.AlternativeCourseProvider

/** 有课程能力等级要求的审核监听
 */
class AuditCourseAbilityListener extends AuditPlanListener {

  var alternativeCourseProvider: AlternativeCourseProvider = _

  var entityDao: EntityDao = _

  /** 开始审核计划
   *
   * @return false 表示不能继续审核
   */
  override def start(context: AuditPlanContext): Unit = {
    val std = context.std
    val rates = entityDao.findBy(classOf[StdCourseAbility], "std", context.std).map(_.rate).toSet
    val subjectRates = rates.groupBy(_.subject)

    val result = context.result
    context.sharePlan foreach { sharePlan =>
      val groups = sharePlan.groups.filter { g =>
        g.courseAbilityRate.nonEmpty
          && rates.contains(g.courseAbilityRate.get)
          && result.getGroupResult(g.courseType.name).nonEmpty
          && result.getGroupResult(g.courseType.name).get.children.isEmpty
      }
      if (groups.nonEmpty) {
        val abilityGroups = Collections.newMap[String, AuditGroupResult]
        groups foreach { g =>
          val groupName = g.courseType.name + " " + g.courseAbilityRate.get.subject.name //例如英语类
          val subjectGroupResult = result.getGroupResult(g.courseType.name).get //等级课程要求所在的组
          val gr = abilityGroups.get(groupName) match {
            case None =>
              val groupResult = new AuditGroupResult(groupName, g.courseType)
              subjectGroupResult.subCount = 1
              groupResult.indexno = subjectGroupResult.indexno + ".1"
              groupResult.planResult = context.result
              groupResult.requiredCredits = g.planCourses.map(pc => if pc.compulsory then pc.course.getCredits(std.level) else 0).sum
              groupResult.requiredCredits = Math.min(groupResult.requiredCredits, subjectGroupResult.requiredCredits) //不能超过上级组的要求学分
              abilityGroups.put(groupName, groupResult)
              result.addGroupResult(groupResult)
              subjectGroupResult.addChild(groupResult)
              groupResult
            case Some(gr) =>
              val required = g.planCourses.map(pc => if pc.compulsory then pc.course.getCredits(std.level) else 0).sum
              if (required < gr.requiredCredits) gr.requiredCredits = required
              gr.requiredCredits = Math.min(gr.requiredCredits, subjectGroupResult.requiredCredits) //不能超过上级组的要求学分
              gr
          }
          g.planCourses foreach { pc =>
            val cr = new AuditCourseResult(pc)
            val rate = pc.group.courseAbilityRate.get
            cr.addRemark(rate.name)
            if (cr.compulsory && subjectRates(rate.subject).size > 1) cr.compulsory = false //有可能等级变动，每个等级的课程不能都是必修的
            val courseGrades = context.stdGrade.useGrade(pc.course)
            cr.updatePassed(courseGrades)
            if cr.passed || pc.compulsory || cr.hasGrade then gr.addCourseResult(cr)
          }
        }
      }
    }

  }
}
