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
import org.openurp.code.edu.model.CourseRank
import org.openurp.edu.grade.domain.GradeFilter
import org.openurp.edu.grade.model.CourseGrade

/** 同名课程过滤
 * 保留必修、限选。
 * 任选课中，筛选成绩成绩好的，学的早的。
 */
class SameNameGradeFilter extends GradeFilter {

  var reservedCourseNames = Collections.newSet[String]

  override def filter(grades: Iterable[CourseGrade]): Iterable[CourseGrade] = {
    if (grades.isEmpty) return grades
    val rs = Collections.newBuffer[CourseGrade]
    val courseNameGrades = grades.groupBy(_.course.name)
    courseNameGrades foreach { case (courseName, gs) =>
      if (gs.size == 1) { //只有一个记录
        rs.addOne(gs.head)
      } else {
        val cs = gs.map(_.course).toSet
        if (cs.size == 1) { //只有一门课
          rs.addAll(gs)
        } else if (reservedCourseNames.contains(courseName)) { //保留课程名称
          rs.addAll(gs)
        } else {
          // 保留全部的必修和限选课程
          val compulsory = gs.filter(g => !g.courseType.optional || g.courseType.rank.map(_.id).contains(CourseRank.DesignatedSelective))
          if (compulsory.isEmpty) {
            val passed = gs.filter(_.passed).toBuffer.sortBy(x => x.courseType.module.map(_.id).getOrElse(0).toString + "_" + x.semester.beginOn.toString)
            val failed = gs.filter(!_.passed).toBuffer.sortBy(x => x.courseType.module.map(_.id).getOrElse(0).toString + "_" + x.semester.beginOn.toString)
            if passed.nonEmpty then rs.addOne(passed.head)
            else rs.addOne(failed.head)
          } else {
            rs.addAll(compulsory) //全部添加
          }
        }
      }
    }
    rs
  }

}
