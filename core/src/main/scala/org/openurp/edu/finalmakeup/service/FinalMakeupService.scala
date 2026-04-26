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

package org.openurp.edu.finalmakeup.service

import org.openurp.base.edu.model.Course
import org.openurp.base.model.{Campus, Project, Semester}
import org.openurp.base.std.model.{Squad, Student}
import org.openurp.edu.exam.model.{FinalMakeupCourse, FinalMakeupTaker}
import org.openurp.edu.grade.model.CourseGrade

/**
 * 毕业补考服务
 */
trait FinalMakeupService {

  def split(makeupCourse: FinalMakeupCourse): Seq[FinalMakeupCourse]

  def getOrCreate(semester: Semester, course: Course, campus: Campus, squad: Option[Squad]): FinalMakeupCourse

  def addTaker(std: Student, course: Course, semester: Semester, mc: Option[FinalMakeupCourse]): String

  def createTaker(std: Student, course: Course, semester: Semester): FinalMakeupTaker

  def update(taker: FinalMakeupTaker): Boolean

  /**
   * 找到可以参见毕业补考的成绩
   *
   * @param project
   * @param semester
   * @return
   */
  def findFailed(project: Project, semester: Semester): Seq[CourseGrade]

  /**
   * 保存补考成绩
   *
   * @param makeupCourse
   * @param grades
   */
  def saveGrades(makeupCourse: FinalMakeupCourse, grades: Iterable[CourseGrade]): Unit
}
