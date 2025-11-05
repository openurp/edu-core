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

import org.beangle.commons.lang.time.Stopwatch
import org.beangle.commons.logging.Logging
import org.beangle.data.dao.OqlBuilder
import org.beangle.data.orm.hibernate.{AbstractDaoTask, SessionHelper}
import org.openurp.base.model.Project
import org.openurp.base.service.ProjectConfigService
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.service.AuditPlanService
import org.openurp.edu.service.Features

import java.time.LocalDate

/** 自动审核计划完成情况
 */
class AutoAuditPlanJob extends AbstractDaoTask, Logging {
  var auditPlanService: AuditPlanService = _
  var projectConfigService: ProjectConfigService = _

  override def execute(): Unit = {
    val projects = entityDao.getAll(classOf[Project]).filter(_.active)
    projects foreach { p =>
      val autoAudit = projectConfigService.get[Boolean](p, Features.Grade.AutoAuditPlan)
      if (autoAudit) {
        val stdIds = getStdIds(p)
        logger.info(s"start auto auditing project ${p.code} ${stdIds.size} students")
        val sw0 = new Stopwatch(true)
        val sw = new Stopwatch(true)
        var cnt = 0
        var i = 0
        var startCode: String = null
        stdIds foreach { stdId =>
          val std = entityDao.get(classOf[Student], stdId)
          if (null == startCode) startCode = std.code
          auditPlanService.audit(std, Map.empty, true)
          cnt += 1
          if cnt % 100 == 0 then
            clean()
            logger.info(s"audit ${startCode}~${std.code} using ${sw}")
            sw.reset().start()
            startCode = null
        }
        logger.info(s"end auto auditing, total ${cnt} using ${sw0}")
      }
    }

  }

  private def getStdIds(p: Project): Seq[Long] = {
    val query = OqlBuilder.from[Long](classOf[Student].getName, "s")
    //在校，有效期内的学籍
    query.where("s.state.beginOn <= :now and s.state.endOn >=:now", LocalDate.now)
    query.where("s.state.inschool=true")
    query.where("s.project=:project", p)
    query.orderBy("s.code")
    query.select("s.id")
    entityDao.search(query)
  }
}
