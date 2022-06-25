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
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.model.CourseGradeState
import org.beangle.data.dao.Operation

/**
 * 成绩发布监听器
 *
 */
trait CourseGradePublishListener {
  /**
   * 发布单个成绩
   *
   * @param grade
   * @param gradeTypes
   * @return
   */
  def onPublish(grade: CourseGrade, gradeTypes: Array[GradeType]): collection.Seq[Operation]

  /**
   * 发布一批成绩
   *
   * @param grades
   * @param gradeState
   * @param gradeTypes
   * @return
   */
  def onPublish(grades: Iterable[CourseGrade], gradeState: CourseGradeState, gradeTypes: Array[GradeType]): collection.Seq[Operation]
}
