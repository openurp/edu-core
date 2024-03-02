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

import org.beangle.commons.bean.DefaultPropertyExtractor
import org.beangle.commons.collection.Collections
import org.openurp.base.hr.model.Teacher
import org.openurp.edu.clazz.model.Clazz
import org.openurp.edu.grade.model.CourseGradeState

class GradeStatExtractor extends DefaultPropertyExtractor {

  override def get(target: Object, property: String): Any = {
    if ("teachers" == property) {
      var teachers = Collections.newBuffer[Teacher]
      if (target.isInstanceOf[Clazz]) {
        val clazz = target.asInstanceOf[Clazz]
        teachers = clazz.teachers
      } else {
        val gradeState = target.asInstanceOf[CourseGradeState]
        teachers = gradeState.clazz.teachers
      }
      if teachers.isEmpty then "未安排教师" else teachers.map(_.name).mkString(",")
    } else {
      super.get(target, property)
    }
  }
}
