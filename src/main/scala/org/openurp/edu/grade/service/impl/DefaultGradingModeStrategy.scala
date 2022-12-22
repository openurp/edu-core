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

import org.beangle.data.dao.EntityDao
import org.beangle.security.Securities
import org.openurp.code.edu.model.{GradeType, GradingMode}
import org.openurp.edu.grade.model.*
import org.openurp.edu.grade.service.GradingModeStrategy

import java.time.Instant

/**
 * 默认成绩记录方式配置方法
 */
class DefaultGradingModeStrategy extends GradingModeStrategy {

  var entityDao: EntityDao = _

  def configGradingMode(gradeState: CourseGradeState, gradeTypes: Iterable[GradeType]): Unit = {
    if (isDefault(gradeState.gradingMode)) gradeState.gradingMode = getDefaultCourseGradeGradingMode(gradeState)
    for (t <- gradeTypes) {
      val typeState = GradingModeStrategy.getOrCreateState(gradeState, t)
      if (null == typeState.gradingMode) {
        typeState.gradingMode = getDefaultExamGradeGradingMode(gradeState, typeState)
      }
    }
    entityDao.saveOrUpdate(gradeState)
  }

  protected def getDefaultCourseGradeGradingMode(state: CourseGradeState): GradingMode = {
    val modes = state.clazz.course.gradingModes
    if (modes.isEmpty) {
      entityDao.get(classOf[GradingMode], GradingMode.Percent)
    } else {
      modes.head
    }
  }

  protected def getDefaultExamGradeGradingMode(gradeState: CourseGradeState, typeState: GradeState): GradingMode = {
    if (typeState.gradeType.isGa) {
      gradeState.gradingMode
    } else {
      if (typeState.gradeType.id == GradeType.Delay) {
        val endGradeState = gradeState.getState(new GradeType(GradeType.End)).asInstanceOf[ExamGradeState]
        if (null == endGradeState) gradeState.gradingMode else endGradeState.gradingMode
      } else {
        entityDao.get(classOf[GradingMode], GradingMode.Percent)
      }
    }
  }

  private def isDefault(m: GradingMode): Boolean = {
    null == m || m.id == GradingMode.Percent
  }
}
