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

import org.openurp.base.std.model.Student
import org.openurp.edu.grade.model.AuditPlanResult

trait AuditPlanService {

  def audit(std: Student, params: collection.Map[String, Any], persist: Boolean = false): AuditPlanResult

  /**
   * 获得学生的计划完成审 核结果<br>
   * 这个计划完成审核结果可能是部分审核的结果，也可能是全部审核的结果<br>
   *
   * @param std
   * @return
   */
  def getResult(std: Student): Option[AuditPlanResult]

  /** 批量审核 */
  def batchAudit(stds: Iterable[Student], params: collection.Map[String, Any]): Unit
}
