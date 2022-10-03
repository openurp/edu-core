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

import org.openurp.code.edu.model.GradeType
import org.openurp.base.model.Project
import org.openurp.edu.grade.model.CourseGradeState
import org.openurp.edu.clazz.model.Clazz

trait CourseGradeService {
  /**
   * 按照成绩状态，重新计算成绩的<br>
   * 1、首先更改成绩的成绩记录方式<br>
   * 2、score以及是否通过和绩点等项<br>
   * 3、如果成绩状态中发布状态，则进行发布操作
   *
   * @param gradeState
   * @return
   */
  def recalculate(gradeState: CourseGradeState): Unit
  /**
   * 删除考试成绩<br>
   * 同时将该成绩和总评成绩的教师确认位置为0
   *
   * @param clazz
   * @param gradeType
   */
  def remove(clazz: Clazz, gradeType: GradeType): Unit
  /**
   * 发布或取消发布成绩
   *
   * @param clazzIdSeq
   * @param gradeTypes
   *          如果为空,则发布影响总评和最终
   * @param published
   */
  def publish(clazzIdSeq: String, gradeTypes: Array[GradeType], published: Boolean): Unit
/**
   * 查询成绩状态
   *
   * @param clazz
   * @return
   */
  def getState(clazz: Clazz): CourseGradeState

  def getPublishableGradeTypes(project: Project): Seq[GradeType]
}
