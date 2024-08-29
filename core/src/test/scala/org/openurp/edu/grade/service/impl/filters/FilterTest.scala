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

package org.openurp.edu.grade.service.impl.filters

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Course
import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.code.edu.model.{CourseTakeType, EducationLevel}
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.StdAlternativeCourse
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

class FilterTest extends AnyFunSpec with Matchers {


  describe("Filter") {
    val std = new Student
    val level = new EducationLevel
    std.level = level

    val grades = Collections.newBuffer[CourseGrade]
    val courseTakeType = new CourseTakeType(1, "1", "初修", "Normal")
    val semester1 = new Semester(1, "20201", "2020-2021", "1", LocalDate.parse("2020-09-01"), LocalDate.parse("2021-02-01"))
    val semester2 = new Semester(2, "20202", "2020-2021", "2", LocalDate.parse("2021-03-01"), LocalDate.parse("2021-08-01"))
    val a = new Course(1, "A", "A", "A")
    val b = new Course(2, "B", "B", "B")
    val c = new Course(3, "C", "C", "C")
    val d = new Course(4, "D", "D", "D")
    List(a, b, c, d) foreach { c => c.defaultCredits = 1 }

    val ga = new CourseGrade(1L, std, a, semester1, "0001").updateScore(90f, "90", 3f, true)
    val gb = new CourseGrade(2L, std, b, semester1, "0001").updateScore(80f, "80", 2f, true)
    val gc = new CourseGrade(3L, std, c, semester2, "0001").updateScore(70f, "70", 10f, true)
    val gd = new CourseGrade(4L, std, d, semester2, "0001").updateScore(0f, "0", 0f, true)
    List(ga, gb, gc, gd).foreach { x =>
      x.courseTakeType = courseTakeType
    }

    it("filter best") {
      val alts = List(StdAlternativeCourse(std, Set(b, c), Set(a)), StdAlternativeCourse(std, Set(a), Set(b)))
      val filter = new AlternativeGradeFilter(alts)
      val result = filter.filter(List(ga, gb, gc))
      result.size should equal(2)
      result foreach { x => println((x.course.name, x.gp.getOrElse(0))) }
    }
    it("filter first") {
      val alts = List(StdAlternativeCourse(std, Set(c), Set(a)))
      val filter = new FirstGradeFilter(alts)
      val result = filter.filter(List(ga, gb, gc))
      result.size should equal(2)
      result foreach { x => println((x.course.name, x.gp.getOrElse(0))) }
    }
  }
}
