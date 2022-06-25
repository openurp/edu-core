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
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.model.CourseGradeState
import org.openurp.edu.grade.model.ExamGrade
import org.openurp.edu.grade.model.GaGrade

/**
 * 成绩计算器
 */
trait CourseGradeCalculator {

  /**
   * 计算最终成绩,是否通过和绩点
   *
   * @param grade
   */
  def calcFinal(grade: CourseGrade, state: CourseGradeState): Unit

  /**
   * 计算最终成绩,是否通过和绩点
   *
   * @param grade
   */
  def calcAll(grade: CourseGrade, state: CourseGradeState): Unit

  /**
   * 计算总评成绩
   *
   * @param grade
   * @return 总评成绩,但不改动成绩
   */
  def calcEndGa(grade: CourseGrade, state: CourseGradeState): GaGrade

  /**
   * 计算补考或者缓考总评成绩
   *
   * @param grade
   * @return 总评成绩,但不改动成绩
   */
  def calcMakeupDelayGa(grade: CourseGrade, state: CourseGradeState): GaGrade

  /**
   * 更新最终分数
   *
   * @param grade
   * @param score
   * @param newStyle
   */
  def updateScore(grade: CourseGrade, score: Option[Float], newStyle: GradingMode): Unit

  /**
   * 更新考试成绩分数
   *
   * @param eg
   * @param score
   * @param newStyle
   */
  def updateScore(eg: ExamGrade, score: Option[Float], newStyle: GradingMode): Unit

  /**
   * 更新总评成绩分数
   *
   * @param gag
   * @param score
   * @param newStyle
   */
  def updateScore(gag: GaGrade, score: Option[Float], newStyle: GradingMode): Unit

  /**
   * 得到用以转换成绩用的服务
   *
   * @return
   */
  def gradeRateService: GradeRateService
}
