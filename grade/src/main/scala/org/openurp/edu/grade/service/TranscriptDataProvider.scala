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

/**
 * 成绩单信息提供者
 *
 *
 * @since 2012-05-21
 */
trait TranscriptDataProvider {

  /**
   * 批量提供学生数据
   *
   * @param stds
   * @param options
   * @return
   */
  def getDatas(stds: Seq[Student], options: collection.Map[String, String]): AnyRef
  /**
   * 提供数据名
   *
   * @return
   */
  def dataName: String
}
