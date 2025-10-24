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
import org.openurp.base.util.FloatSegment
import org.openurp.code.edu.model.{ExamStatus, GradeType}
import org.openurp.edu.grade.model.{CourseGrade, ExamGrade, Grade}

object GradeSegStat {
  def getDefaultSegments: Seq[FloatSegment] = {
    val segments = Collections.newBuffer[FloatSegment]
    segments += new FloatSegment(90, 100)
    segments += new FloatSegment(80, 89)
    segments += new FloatSegment(70, 79)
    segments += new FloatSegment(60, 69)
    segments += new FloatSegment(50, 59)
    segments += new FloatSegment(0, 49)
    segments.toSeq
  }

  def apply(gs: collection.Seq[Grade], gradeType: GradeType, segments: Iterable[FloatSegment]): GradeSegStat = {
    val segs = segments.map(_.clone)
    var grades = gs
    if (!gradeType.isGa && gradeType.id > 0) {
      grades = gs.filter(x => x.asInstanceOf[ExamGrade].examStatus.attended)
    }
    if (grades.isEmpty) {
      new GradeSegStat(gradeType, segs, 0, 0, 0, 0)
    } else {
      val stdCount = grades.size
      grades = grades.sortBy(_.score.getOrElse(0f))
      val lowest = grades.head.score.getOrElse(0f)
      val highest = grades.last.score.getOrElse(0f)
      var sum = 0L
      var average = 0f
      grades foreach { grade =>
        grade.score foreach { score =>
          sum += (score * 100).intValue
          segs.find(_.contains(score)).foreach { seg => seg.count += 1 }
        }
      }
      if 0 != stdCount then average = (BigDecimal(sum) / BigDecimal(stdCount * 100)).floatValue
      new GradeSegStat(gradeType, segs, stdCount, highest, lowest, average)
    }
  }

  def stat(courseGrades: Iterable[CourseGrade], gradeTypes: Iterable[GradeType], segments: Seq[FloatSegment]): Seq[GradeSegStat] = {
    val gradeSegStats = Collections.newBuffer[GradeSegStat]
    val normalStatus = new ExamStatus(ExamStatus.Normal)
    for (gradeType <- gradeTypes) {
      val grades = Collections.newBuffer[Grade]
      for (courseGrade <- courseGrades) {
        if (gradeType.id == GradeType.Final) grades += courseGrade
        else {
          var examStatus = normalStatus
          courseGrade.getGrade(gradeType) foreach { eg =>
            eg match
              case eg: ExamGrade => examStatus = eg.examStatus
              case _ =>
            if (eg.score.nonEmpty && examStatus == normalStatus) grades += eg
          }
        }
      }
      if (grades.nonEmpty) gradeSegStats += GradeSegStat(grades, gradeType, segments)
    }
    gradeSegStats.toSeq
  }
}

/**
 * 成绩分段统计
 */
class GradeSegStat(val gradeType: GradeType, val segments: Iterable[FloatSegment], val stdCount: Int, val highest: Float, val lowest: Float, val average: Float) {

  def getSegment(min: Number, max: Number): Option[FloatSegment] = {
    segments.find(x => x.min == min.floatValue && x.max == max.floatValue)
  }

}
