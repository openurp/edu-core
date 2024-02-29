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

package org.openurp.edu.grade.service.impl

import org.beangle.commons.collection.Collections
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.model.CourseGradeState
import org.openurp.edu.grade.service.CourseGradeCalculator
import org.openurp.edu.grade.service.CourseGradePublishListener
import org.beangle.data.dao.Operation

class RecalcGpPublishListener extends CourseGradePublishListener {

  var calculator: CourseGradeCalculator = _

  def onPublish(grade: CourseGrade, gradeTypes: Iterable[GradeType]): collection.Seq[Operation] = {
    if (gradeTypes.exists(x => x.id == GradeType.MakeupGa || x.id == GradeType.DelayGa)) {
      calculator.calcMakeupDelayGa(grade, null)
      Operation.saveOrUpdate(grade).build()
    } else {
      List.empty
    }
  }

  def onPublish(grades: Iterable[CourseGrade], gradeState: CourseGradeState, gradeTypes: Iterable[GradeType]): collection.Seq[Operation] = {
    val operations = Collections.newBuffer[Operation]
    var hasMakeupOrDelay = false
    for (
      gradeType <- gradeTypes if (gradeType.id == GradeType.MakeupGa || gradeType.id == GradeType.DelayGa)
        && !hasMakeupOrDelay
    ) {
      hasMakeupOrDelay = true
    }
    if (!hasMakeupOrDelay) return operations
    for (grade <- grades) {
      calculator.calcMakeupDelayGa(grade, gradeState)
      operations ++= Operation.saveOrUpdate(grade).build()
    }
    operations
  }
}
