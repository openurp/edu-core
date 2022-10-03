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

import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.clazz.model.Clazz

/**
 * 补缓成绩设置策略
 */
trait MakeupStdStrategy {

  def getClazzCondition(gradeTypeId: Int): String

  def getCourseTakers(clazz: Clazz): Seq[CourseTaker]

   /**
   * 每个任务的补缓人数
   *
   * @param clazzes
   * @return
   */
  def getCourseTakerCounts(clazzes: Seq[Clazz]): collection.Map[Clazz, Number]
}
