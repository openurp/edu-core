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

import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.model.CourseGrade
/**
 * 平均绩点计算服务
 */
trait GpaService {

  /**
   * 统计学生的在校所有学期的平均绩点
   *
   * <pre>
   *      平均绩点为： gpa=(∑(绩点*学分))/∑(学分)
   *      平均分为： ga=(∑(得分*学分))/∑(学分)
   * </pre>
   *
   * @param std
   * @return
   */
  def getGpa(std: Student): Float

  /**
   * 统计学生的平均绩点<br>
   * 平均绩点为： gpa=(∑(绩点*学分))/∑(学分) 平均绩点以截断的方式保留后面两位
   *
   * @param std
   * @param grades
   *          可以为null
   * @return
   */
  def getGpa(std: Student, grades: collection.Iterable[CourseGrade]): Float

  /**
   * 统计学生的平均绩点<br>
   * 除"学生"之外的其他参数均为可选参数。<br>
   * 平均绩点为： gpa=(∑(绩点*学分))/∑(学分) 平均绩点以截断的方式保留后面两位
   *
   * @param std
   * @param semester
   *          可以为null
   * @return
   */
  def getGpa(std: Student, semester: Semester): Float
}
