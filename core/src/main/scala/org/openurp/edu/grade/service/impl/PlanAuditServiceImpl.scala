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

package org.openurp.edu.grade.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.domain.{DefaultPlanAuditor, PlanAuditContext, PlanAuditListener, StdGrade}
import org.openurp.edu.grade.model.PlanAuditResult
import org.openurp.edu.grade.service.PlanAuditService
import org.openurp.edu.program.domain.CoursePlanProvider

class PlanAuditServiceImpl extends DefaultPlanAuditor with PlanAuditService with Logging {

  var entityDao: EntityDao = _

  protected var coursePlanProvider: CoursePlanProvider = _
  protected var courseGradeProvider: CourseGradeProvider = _

  protected var listeners = Collections.newBuffer[PlanAuditListener]

  def audit(std: Student, params: collection.Map[String, Any], persist: Boolean): PlanAuditResult = {
    logger.debug("start audit " + std.code)
    val coursePlan = coursePlanProvider.getCoursePlan(std).get
    val stdGrade = new StdGrade(courseGradeProvider.getPublished(std))
    val context = new PlanAuditContext(std, coursePlan, stdGrade, this.listeners)
    context.params ++= params

    val planAuditResult = audit(context)
    if (persist) {
      PlanAuditPersister.save(planAuditResult, entityDao)
    }
    planAuditResult
  }

  def getResult(std: Student): Option[PlanAuditResult] = {
    val query = OqlBuilder.from(classOf[PlanAuditResult], "planResult")
    query.where("planResult.std = :std", std)
    entityDao.search(query).headOption
  }

  def batchAudit(stds: Iterable[Student], params: collection.Map[String, Any]): Unit = {
    stds foreach { std =>
      audit(std, params, true)
    }
  }

}
