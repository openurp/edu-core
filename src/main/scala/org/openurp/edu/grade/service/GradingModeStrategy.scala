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

import org.beangle.security.Securities
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.grade.model.{CourseGradeState, ExamGradeState, GaGradeState, Grade, GradeState}

import java.time.Instant

object GradingModeStrategy {

  def getOrCreateState(gs: CourseGradeState, gradeType: GradeType): GradeState = {
    if (gradeType.isGa) {
      gs.gaStates find (_.gradeType.id == gradeType.id) match {
        case None =>
          val result = new GaGradeState
          result.status = Grade.Status.New
          result.updatedAt = Instant.now
          result.gradeType = gradeType
          result.gradeState = gs
          result.operator = Securities.user
          gs.gaStates += result
          result
        case Some(result) => result
      }
    } else {
      gs.examStates find (_.gradeType.id == gradeType.id) match {
        case None =>
          val result = new ExamGradeState
          result.status = Grade.Status.New
          result.updatedAt = Instant.now
          result.gradeType = gradeType
          result.gradeState = gs
          result.operator = Securities.user
          gs.examStates += result
          result
        case Some(result) => result
      }
    }
  }
}

/**
 * 课程成绩记录方式配置策略
 *
 *
 */
trait GradingModeStrategy {

  /**
   * 针对空白的记录方式进行设置默认值
   *
   * @param gradeState
   * @param gradeTypes
   */
  def configGradingMode(gradeState: CourseGradeState, gradeTypes: Iterable[GradeType]): Unit

}
