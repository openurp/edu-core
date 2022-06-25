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
import org.beangle.data.transfer.exporter.DefaultPropertyExtractor
import org.openurp.base.edu.model.Teacher
import org.openurp.edu.grade.model.CourseGradeState
import org.openurp.edu.clazz.model.Clazz

class GradeStatExtractor  extends DefaultPropertyExtractor {

  override def getPropertyValue(target: Object, property: String): Any = {
    if ("teachers" == property) {
      var teacherName = ""
      var teachers = Collections.newBuffer[Teacher]
      if (target.isInstanceOf[Clazz]) {
        val clazz = target.asInstanceOf[Clazz]
        teachers = clazz.teachers
      } else {
        val gradeState = target.asInstanceOf[CourseGradeState]
        teachers = gradeState.clazz.teachers
      }
      if (teachers.size == 0) {
        return "未安排教师"
      }
      for (i <- 0 until teachers.size) {
        if (i > 0) {
          teacherName += ","
        }
        teacherName += teachers(i).user.name
      }
      teacherName
    } else {
      super.getPropertyValue(target, property)
    }
  }
}
