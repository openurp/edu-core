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

import org.beangle.data.dao.OqlBuilder
import org.beangle.commons.script.ExpressionEvaluator
import org.openurp.code.edu.model.GradingMode
import org.openurp.base.model.Project
import org.openurp.edu.grade.config.GradeRateConfig
import org.openurp.edu.grade.config.GradeRateItem
import org.openurp.edu.grade.service.GradeRateService
import org.openurp.edu.grade.service.ScoreConverter
import org.beangle.commons.collection.Collections
import org.openurp.edu.grade.BaseServiceImpl

class GradeRateServiceImpl extends BaseServiceImpl with GradeRateService {

  private var expressionEvaluator: ExpressionEvaluator = _

  /**
   * 查询记录方式对应的配置
   */
  def getConverter(project: Project, gradingMode: GradingMode): ScoreConverter = {
    if (null == project || null == gradingMode) {
      throw new IllegalArgumentException("require project and grade and grading option ")
    }
    val builder = OqlBuilder.from(classOf[GradeRateConfig], "config")
      .where("config.project=:project and config.gradingMode=:gradingMode", project, gradingMode)
      .cacheable()
    val config = entityDao.unique(builder)
    if (null == config) throw new RuntimeException("Cannot find ScoreConverter for " + gradingMode.name)
    new ScoreConverter(config, expressionEvaluator)
  }

  def getGradeItems(project: Project): collection.Map[GradingMode, collection.Seq[GradeRateItem]] = {
    val builder = OqlBuilder.from(classOf[GradeRateConfig], "config")
      .where("config.project=:project and config.gradingMode.numerical=false", project)
    val configs = entityDao.search(builder)
    val datas = Collections.newMap[GradingMode, collection.mutable.Buffer[GradeRateItem]]
    for (config <- configs) {
      val items = datas.getOrElseUpdate(config.gradingMode, Collections.newBuffer[GradeRateItem])
      items ++= config.items
    }
    datas
  }

  /**
   * 获得支持的记录方式
   *
   * @param project
   * @return
   */
  def getGradingModes(project: Project): Seq[GradingMode] = {
    val builder = OqlBuilder.from[GradingMode](classOf[GradeRateConfig].getName, "config")
      .where("config.project=:project", project)
      .select("config.gradingMode")
      .cacheable()
    entityDao.search(builder)
  }
}
