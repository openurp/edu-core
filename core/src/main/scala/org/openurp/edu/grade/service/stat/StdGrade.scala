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

package org.openurp.edu.grade.service.stat

import org.beangle.commons.collection.Collections
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.GradeFilter
import org.openurp.edu.grade.model.{CourseGrade, StdGpa}

/**
 * 学生成绩单打印模型
 *
 */
class StdGrade {

  var std: Student = _

  var grades: collection.Iterable[CourseGrade] = _

  var gradeFilters: List[GradeFilter] = _

  var stdGpa: StdGpa = _

  var cmp: Ordering[CourseGrade] = _

  /**
   * 将grades转换成[course.id.toString,courseGrade]样式的map<br>
   * 主要用于快速根据课程找到成绩.对于重修课程(课程出现重复)对应的成绩是不可预知的. FIXME
   * @return
   */
  def toGradeMap(): collection.Map[String, CourseGrade] = {
    if (null == grades || grades.isEmpty) {
      Map.empty
    } else {
      val gradeMap = Collections.newMap[String, CourseGrade]
      grades foreach { g =>
        gradeMap.put(g.course.id.toString, g)
      }
      gradeMap
    }
  }

  def this(std: Student,
           courseGrades: Seq[CourseGrade],
           cmp: Ordering[CourseGrade],
           gradeFilters: List[GradeFilter]) = {
    this()
    this.std = std
    this.gradeFilters = gradeFilters
    this.grades = courseGrades
    if (null != gradeFilters) {
      for (filter <- gradeFilters) {
        grades = filter.filter(grades)
      }
    }
    if (null != cmp) {
      grades = grades.toBuffer.sorted(cmp)
    }
    this.cmp = cmp
  }

  def filterGrade(gradeFilter: GradeFilter): Unit = {
    if (null != gradeFilter) {
      grades = gradeFilter.filter(grades)
    }
  }

  /**
   * 计算学生已经获得的学分(成绩合格)
   * @return
   */
  def credits: Float = {
    if (null == grades || grades.isEmpty) {
      return 0f
    }
    val level = grades.head.std.level
    var credits = 0f
    grades.foreach { g =>
      if (g.passed) {
        credits += g.course.getCredits(level)
      }
    }
    credits
  }

  def addGrade(grade: CourseGrade): Unit = {
    this.grades = (this.grades.toBuffer += grade)
  }

}
