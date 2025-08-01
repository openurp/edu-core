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

package org.openurp.edu.course.service

import org.openurp.edu.course.flow.NewCourseApply

/** 新开课程服务
 */
trait NewCourseService {

  /** 检查新课申请是否存在问题
   *
   * @param apply
   * @return
   */
  def check(apply: NewCourseApply): Seq[String]

  /** 根据课程申请信息，生成一个新代码
   *
   * @param apply
   * @return
   */
  def generate(apply: NewCourseApply): String
}
