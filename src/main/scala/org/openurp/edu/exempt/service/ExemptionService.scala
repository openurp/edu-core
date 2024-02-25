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

package org.openurp.edu.exempt.service

import org.openurp.base.edu.model.Course
import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.edu.extern.model.{CertificateGrade, ExternGrade}
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.{CoursePlan, PlanCourse, Program}

trait ExemptionService {

  def calcExemptScore(cg: CertificateGrade): Option[Float]

  def getSemester(program: Program, term: Option[Int]): Option[Semester]

  def getConvertablePlanCourses(std: Student, plan: CoursePlan): Seq[PlanCourse]

  def getConvertedGrades(std: Student, courses: collection.Iterable[Course]): Seq[CourseGrade]

  def removeExemption(cg: CertificateGrade, course: Course): Unit

  def removeExemption(eg: ExternGrade, course: Course): Unit

  def addExemption(eg: ExternGrade, ecs: Iterable[Course], score: Option[Float]): Unit

  def addExemption(cg: CertificateGrade, ecs: Iterable[Course], score: Option[Float]): Unit
}
