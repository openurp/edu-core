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

package org.openurp.edu.grade.app.model

import org.beangle.data.model.LongId
import org.openurp.base.edu.model.Course
import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.code.edu.model.{ExamStatus, GradeType}

import java.time.Instant

/**
 * 成绩新增/修改记录
 *
 */
class CourseGradeLog extends LongId {

  var std: Student = _

  var course: Course = _

  var semester: Semester = _

  var gradeType: GradeType = _

  var oldScore: String = _

  var newScore: String = _

  var oldExamStatus: ExamStatus = _

  var newExamStatus: ExamStatus = _

  var gradeId: Long = _

  var updatedAt: Instant = _

  var operator: String = _

  var updatedFrom: String = _

  var removed: Boolean = _

  var remark: String = _
}
