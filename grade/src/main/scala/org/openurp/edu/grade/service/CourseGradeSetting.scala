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
import org.openurp.base.model.Project
import org.openurp.code.edu.model.{ExamStatus, GradeType}

class CourseGradeSetting {

  /** 总评成绩的组成部分 */
  var gaElementTypes = Collections.newBuffer[GradeType]

  /** 允许补考考试类型 */
  var allowExamStatuses = Collections.newSet[ExamStatus]

  /** 不允许录入成绩的考试类型列表 */
  var emptyScoreStatuses = Collections.newSet[ExamStatus]

  /** 是否提交即发布 */
  var submitIsPublish = false

  var delayIsGa = false

  def this(project: Project) = {
    this()
  }
}
