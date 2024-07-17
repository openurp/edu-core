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

import org.openurp.base.edu.model.{Course, TeachingOffice}
import org.openurp.base.hr.model.Teacher
import org.openurp.base.model.{Department, Project, Semester, User}
import org.openurp.edu.course.model.CourseTask

trait CourseTaskService {

  def statTask(project: Project, semester: Semester): Int

  def isDirector(course: Course, teacher: Teacher): Boolean

  def isDirector(semester: Semester, course: Course, teacher: Teacher): Boolean

  def getOffice(semester: Semester, course: Course, depart: Department): Option[TeachingOffice]

  def getOfficeDirector(semester: Semester, course: Course, depart: Department): Option[User]

  def getTasks(project: Project, semester: Semester, teacher: Teacher): Seq[CourseTask]
}
