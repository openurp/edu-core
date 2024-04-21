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

package org.openurp.edu.ws.grade

import org.beangle.commons.lang.time.Stopwatch
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.{LimitQuery, OqlBuilder, QueryPage}
import org.beangle.data.orm.hibernate.DaoJob
import org.openurp.base.model.Project
import org.openurp.base.service.ProjectConfigService
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.service.AuditPlanService
import org.openurp.edu.service.Features

import java.time.LocalDate

class AutoAuditJob extends DaoJob, Logging {
  var auditPlanService: AuditPlanService = _
  var projectConfigService: ProjectConfigService = _

  override def execute(): Unit = {
    val projects = entityDao.getAll(classOf[Project]).filter(_.active)
    projects foreach { p =>
      val autoAudit = projectConfigService.get[Boolean](p, Features.Grade.AutoAuditPlan)
      if (autoAudit) {
        logger.info(s"start auto auditing")
        val query = OqlBuilder.from(classOf[Student], "s")
        //在校，有效期内的学籍
        query.where("s.state.beginOn <= :now and s.state.endOn >=:now", LocalDate.now)
        query.where("s.state.inschool=true")
        query.where("s.project=:project", p)
        query.orderBy("s.code")
        query.limit(1, 100)
        val sw = new Stopwatch(true)
        var cnt = 0
        val results = new QueryPage(query.build().asInstanceOf[LimitQuery[Student]], entityDao)
        results foreach { std =>
          auditPlanService.audit(std, Map.empty, true)
          cnt += 1
        }
        logger.info(s"audit ${cnt} using ${sw}")
      }
    }
  }
}
