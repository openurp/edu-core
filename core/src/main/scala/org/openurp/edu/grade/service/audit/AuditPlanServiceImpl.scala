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

import org.beangle.cdi.{Container, ContainerAware}
import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.service.ProjectConfigService
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.*
import org.openurp.edu.grade.model.AuditPlanResult
import org.openurp.edu.grade.service.AuditPlanService
import org.openurp.edu.program.domain.CoursePlanProvider
import org.openurp.edu.service.Features

class AuditPlanServiceImpl extends DefaultPlanAuditor, AuditPlanService, Logging, ContainerAware {

  var entityDao: EntityDao = _

  var coursePlanProvider: CoursePlanProvider = _
  var courseGradeProvider: CourseGradeProvider = _
  var projectConfigService: ProjectConfigService = _

  var container: Container = _
  var listeners: Map[Int, Seq[AuditPlanListener]] = Map.empty

  var defaultListenerNames: String = _

  def audit(std: Student, params: collection.Map[String, Any], persist: Boolean): AuditPlanResult = {
    val existResults = entityDao.findBy(classOf[AuditPlanResult], "std", std).headOption
    if (existResults.nonEmpty && existResults.get.archived) {
      return existResults.get
    }

    logger.debug("start audit " + std.code)
    val coursePlan = coursePlanProvider.getCoursePlan(std).orNull
    val sharePlan = coursePlanProvider.getSharePlan(std)
    val stdGrade = new StdGrade(courseGradeProvider.getPublished(std))
    val projectListeners = listeners.get(std.project.id) match
      case None =>
        val s = projectConfigService.get[String](std.project, Features.Grade.AuditPlanRules)
        val lsnNames = if (s.trim() == "default") then defaultListenerNames else s
        val lsners = Strings.split(lsnNames, ",").flatMap(n => container.getBean[AuditPlanListener]("auditPlanListener." + n)).toSeq
        listeners += (std.project.id -> lsners)
        lsners
      case Some(lsn) => lsn

    val context = new AuditPlanContext(std, coursePlan, sharePlan, stdGrade, projectListeners)
    context.params ++= params

    val newResult = audit(context)
    if (persist) {
      val rs = if existResults.isEmpty then newResult else AuditPlanMerger.merge(newResult, existResults.get)
      entityDao.saveOrUpdate(rs)
      rs
    } else {
      newResult
    }
  }

  private def getResult(std: Student, entityDao: EntityDao): Option[AuditPlanResult] = {
    val query = OqlBuilder.from(classOf[AuditPlanResult], "planResult")
    query.where("planResult.std = :std", std)
    entityDao.search(query).headOption
  }

  def getResult(std: Student): Option[AuditPlanResult] = {
    val query = OqlBuilder.from(classOf[AuditPlanResult], "planResult")
    query.where("planResult.std = :std", std)
    entityDao.search(query).headOption
  }

  def batchAudit(stds: Iterable[Student], params: collection.Map[String, Any]): Unit = {
    stds foreach { std =>
      audit(std, params, true)
    }
  }

}
