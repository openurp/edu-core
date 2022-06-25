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

import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.service.GpaService
import org.openurp.edu.grade.domain.DefaultGpaPolicy

class DefaultGpaService extends GpaService {

  var gpaPolicy = new DefaultGpaPolicy()

  var courseGradeProvider: CourseGradeProvider = _

  def getGpa(std: Student): Float = {
    gpaPolicy.calcGpa(courseGradeProvider.getPublished(std))
  }

  def getGpa(std: Student, grades: collection.Iterable[CourseGrade]): Float = {
    gpaPolicy.calcGpa(grades)
  }

  def getGpa(std: Student, semester: Semester): Float = {
    gpaPolicy.calcGpa(courseGradeProvider.getPublished(std, semester))
  }

}
