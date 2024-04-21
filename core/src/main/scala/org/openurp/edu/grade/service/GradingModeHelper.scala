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

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Numbers
import org.beangle.data.dao.EntityDao
import org.openurp.code.edu.model.GradingMode
import org.openurp.edu.grade.config.GradeRateConfig

class GradingModeHelper {

  var gradeStyles = Collections.newMap[String, GradingMode]

  var styles = Collections.newMap[String, GradingMode]

  var defaultGradingMode: GradingMode = _

  var entityDao: EntityDao = _

  def init(defaultGradingModeId: Int): Unit = {
    val configs = entityDao.getAll(classOf[GradeRateConfig])
    for (config <- configs if !config.gradingMode.numerical) {
      val items = config.items
      for (item <- items) {
        item.grade foreach { g =>
          gradeStyles.put(g, config.gradingMode)
        }
      }
    }
    val mss = entityDao.getAll(classOf[GradingMode])
    for (style <- mss) {
      styles.put(style.code, style)
    }
    if (defaultGradingModeId > 0) {
      defaultGradingMode = entityDao.get(classOf[GradingMode], defaultGradingModeId)
    }
  }

  def styleForCode(code: String): GradingMode = {
    styles.getOrElse(code, defaultGradingMode)
  }

  def styleForScore(score: String): GradingMode = {
    gradeStyles.get(score) match {
      case None => if (Numbers.isDigits(score)) defaultGradingMode else null
      case Some(s) => s
    }
  }

}
