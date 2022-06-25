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

import org.beangle.commons.bean.orderings.PropertyOrdering
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Objects
import org.openurp.base.edu.model.Course
import org.openurp.base.model.Semester
import org.openurp.base.std.model.{Squad, Student}
import org.openurp.edu.grade.model.CourseGrade

/**
 * 多名学生的成绩打印模型<br>
 * 通常以一个班级为单位
 *
 *
 */
class MultiStdGrade(var semester: Semester, grades: collection.Map[Student, Seq[CourseGrade]], var ratio: Float) {

  var squad: Squad = _

  var courses = Collections.newBuffer[Course]

  // [stdGrade列表]
  var stdGrades: Seq[StdGrade] = null

  // 每个学生除了共同课程之外的其他课程[std.id.toString,courseGrades]
  var extraGradeMap = Collections.newMap[String, collection.Seq[CourseGrade]]

  // 最大显示列
  var maxDisplay: Int = _

  if (!grades.isEmpty) {
    val gradesMap = Collections.newMap[Long, StdGrade]
    val courseStdNumMap = Collections.newMap[Course, CourseStdNum]
    // 组装成绩,把每一个成绩放入对应学生的stdGrade中,并记录每一个成绩中课程对应的学生人数.
    for ((key, value) <- grades) {
      val stdGrade = new StdGrade(key, value, null, null)
      gradesMap.put(key.id, stdGrade)
      for (grade <- value) {
        val courseStdNum = courseStdNumMap.get(grade.course).asInstanceOf[CourseStdNum]
        if (null == courseStdNum) {
          courseStdNumMap.put(grade.course, new CourseStdNum(grade.course, 1))
        } else {
          courseStdNum.count = courseStdNum.count + 1
        }
      }
    }

    stdGrades = gradesMap.values.toSeq
    // 按照课程人数倒序排列,找到符合人数底线的公共课程
    val courseStdNums = courseStdNumMap.values.toSeq.sorted
    var maxStdCount = 0

    if (Collections.isNotEmpty(courseStdNums)) {
      maxStdCount = (courseStdNums.head).count
    }

    for (i <- 0 until courseStdNums.size) {
      val rank = courseStdNums(i)
      if (rank.count.toFloat / maxStdCount > ratio) {
        courses += rank.course
      }
    }

    var maxExtra = 0
    // 记录每个学生超出公共课程的成绩,并找出最大的显示多余列
    var iter = stdGrades.iterator
    while (iter.hasNext) {
      val stdGrade = iter.next()
      var myExtra = 0
      val extraGrades = Collections.newBuffer[CourseGrade]
      val commonCourseSet = Collections.newSet[Course]
      var iterator = stdGrade.grades.iterator
      while (iterator.hasNext) {
        val courseGrade = iterator.next()
        if (!commonCourseSet.contains(courseGrade.course)) {
          extraGrades += courseGrade
          myExtra += 1
        }
      }
      if (myExtra > maxExtra) {
        maxExtra = myExtra
      }
      maxDisplay = courses.size + maxExtra
      if (extraGrades.nonEmpty) {
        extraGradeMap.put(stdGrade.std.id.toString, extraGrades)
      }

    }
  }

  def sortStdGrades(cmpWhat: String, isAsc: Boolean): Unit = {
    if (null != stdGrades) {
      val cmp = new PropertyOrdering(cmpWhat, isAsc)
      stdGrades = stdGrades.sorted(cmp)
    }
  }

  def extraCourseNum: Int = {
    maxDisplay - courses.size
  }
}

class CourseStdNum(val course: Course, var count: Int) extends Ordered[CourseStdNum] {

  def compare(o: CourseStdNum): Int = {
    Objects.compareBuilder.add(o.count, this.count)
      .toComparison()
  }
}
