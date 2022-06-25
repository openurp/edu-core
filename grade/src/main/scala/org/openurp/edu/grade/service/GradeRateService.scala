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

import org.openurp.code.edu.model.GradingMode
import org.openurp.base.model.Project
import org.openurp.edu.grade.config.GradeRateItem

/**
 * 绩点规则服务类
 * <p>
 * 提供以下服务
 * <li>转换成绩 convert</li>
 * <li>计算绩点 calGp</li>
 * <li>判断是否通过</li>
 * <li>查询支持的记录方式</li>
 */
trait GradeRateService {

  def getConverter(project: Project, gradingMode: GradingMode): ScoreConverter

  def getGradeItems(project: Project): collection.Map[GradingMode, collection.Seq[GradeRateItem]]

  /**
   * 查询该项目对应的记录方式
   *
   * @param project
   * @return
   */
  def getGradingModes(project: Project): collection.Seq[GradingMode]
}
