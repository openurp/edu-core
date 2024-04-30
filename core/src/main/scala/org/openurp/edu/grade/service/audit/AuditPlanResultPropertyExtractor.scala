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

import org.beangle.commons.bean.DefaultPropertyExtractor
import org.openurp.edu.grade.model.AuditPlanResult

class AuditPlanResultPropertyExtractor extends DefaultPropertyExtractor {

  override def get(target: Object, property: String): Any = {
    val result = target.asInstanceOf[AuditPlanResult]
    property match
      case "result" =>
        result.topGroupResults.filter(!_.passed).map { g =>
          val child = g.children.filter(!_.passed).map(x => x.name + " " + x.owedCredits + "分").mkString(" ")
          g.name + " " + g.owedCredits + "分" + (if child.isEmpty then "" else "(" + child + ")")
        }.mkString("\r\n")
      case "result3" =>
        result.topGroupResults.filter(_.owedCredits3 > 0).map { g =>
          val child = g.children.filter(_.owedCredits3 > 0).map(x => x.name + " " + x.owedCredits3 + "分").mkString(" ")
          g.name + " " + g.owedCredits3 + "分" + (if child.isEmpty then "" else "(" + child + ")")
        }.mkString("\r\n")
      case _ => super.get(target, property)
  }
}
