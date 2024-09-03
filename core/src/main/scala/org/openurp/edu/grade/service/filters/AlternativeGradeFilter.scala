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

package org.openurp.edu.grade.service.filters

import org.beangle.commons.collection.Collections
import org.openurp.base.edu.model.Course
import org.openurp.code.edu.model.{CourseTakeType, GradeType}
import org.openurp.edu.grade.domain.{GradeComparator, GradeFilter}
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.AlternativeCourse

import java.lang as jl
import java.time.LocalDate

object AlternativeGradeFilter {

  class AlternativeGroup {
    val alternatives = Collections.newBuffer[AlternativeCourse]
    val grades = Collections.newBuffer[AlternativeGrade]

    private val courses = Collections.newSet[Set[Course]]

    def add(sc: AlternativeCourse, oGrade: AlternativeGrade, nGrade: AlternativeGrade): Unit = {
      this.alternatives.addOne(sc)
      if (!courses.contains(oGrade.courses)) {
        courses.addOne(oGrade.courses)
        grades.addOne(oGrade)
      }
      if (!courses.contains(nGrade.courses)) {
        courses.addOne(nGrade.courses)
        grades.addOne(nGrade)
      }
    }
  }

  class AlternativeGrade(val score: Float, val credits: Float, gp: Double, gs: Double, val fullGrade: Boolean,
                         val courses: Set[Course], val lastAcquiredOn: LocalDate) extends Ordered[AlternativeGrade] {
    val gpa: Double = if 0 == credits then gp else gp / credits
    val ga: Double = if 0 == credits then gs else gs / credits

    def gaPassed(gradeMap: Map[Long, CourseGrade]): Boolean = {
      if !fullGrade then return false
      !courses.exists { c =>
        gradeMap.get(c.id) match
          case None => true
          case Some(g) => !g.gaGrades.exists(x => x.gradeType.id != GradeType.MakeupGa && x.passed)
      }
    }

    /** 小的放前面,最好的放在最后
     * fullGrade,gpa,ga
     */
    override def compare(other: AlternativeGrade): Int = {
      if (!fullGrade && other.fullGrade) {
        -1
      } else if (fullGrade && !other.fullGrade) {
        1
      } else {
        val gpaCmp = jl.Double.compare(gpa, other.gpa)
        if (gpaCmp == 0) {
          var gaCmp = jl.Double.compare(ga, other.ga)
          if (gaCmp == 0) {
            gaCmp = jl.Float.compare(score, other.score)
            if (gaCmp == 0) {
              gaCmp = other.lastAcquiredOn.compareTo(lastAcquiredOn) //时间按照倒序来
            }
          }
          gaCmp
        } else {
          gpaCmp
        }
      }
    }
  }

  def analysis(substituteCourses: collection.Seq[AlternativeCourse], gradeMap: collection.Map[Course, CourseGrade]): collection.Seq[AlternativeGroup] = {
    if (substituteCourses.isEmpty || gradeMap.isEmpty) {
      return Seq.empty
    }
    val groups = Collections.newBuffer[AlternativeGroup]
    val alternativeGradeMap = Collections.newMap[collection.Set[Course], AlternativeGrade]
    val iter = substituteCourses.iterator
    while (iter.hasNext) {
      val subCourse = iter.next()
      val oGrade = alternativeGradeMap.getOrElseUpdate(subCourse.olds, build(subCourse.olds, gradeMap))
      val nGrade = alternativeGradeMap.getOrElseUpdate(subCourse.news, build(subCourse.news, gradeMap))
      val matched = groups.find { group =>
        val rs = group.alternatives.find(sc => subCourse.news == sc.olds || subCourse.olds == sc.news)
        rs.nonEmpty
      }

      matched match
        case None =>
          val ng = new AlternativeGroup
          groups.addOne(ng)
          ng.add(subCourse, oGrade, nGrade)
        case Some(group) =>
          group.add(subCourse, oGrade, nGrade)
    }
    groups
  }

  private def build(origins: collection.Set[Course], gradeMap: collection.Map[Course, CourseGrade]) = {
    var score = 0f
    var gp = 0d
    var credits = 0f
    var gs = 0d
    var fullGrade = true
    var acquiredOn: LocalDate = null
    val it1 = origins.iterator
    while (it1.hasNext) {
      val course = it1.next
      gradeMap.get(course) match
        case Some(grade) =>
          if (grade.courseTakeType.id != CourseTakeType.Exemption) {
            //登记成绩的最晚获得时间
            if (null == acquiredOn) acquiredOn = grade.semester.endOn
            else if (grade.semester.endOn.isAfter(acquiredOn)) acquiredOn = grade.semester.endOn
            val c = grade.credits
            grade.score foreach { x =>
              score += x
              gs += c * x
            }
            gp += c * grade.gp.getOrElse(0f)
            credits += c
          }
        case None => fullGrade = false
    }
    if (null == acquiredOn) acquiredOn = LocalDate.now

    if 0 == credits then new AlternativeGrade(score, credits, gp, gs, false, Set.empty, acquiredOn)
    else new AlternativeGrade(score, credits, gp, gs, fullGrade, origins.toSet, acquiredOn)
  }
}

class AlternativeGradeFilter(alternativeCourses: collection.Seq[AlternativeCourse]) extends GradeFilter {

  protected def buildGradeMap(grades: Iterable[CourseGrade]): collection.mutable.Map[Course, CourseGrade] = {
    val gradesMap = Collections.newMap[Course, CourseGrade]
    var old: CourseGrade = null
    for (grade <- grades) {
      old = gradesMap.get(grade.course).orNull
      if (GradeComparator.betterThan(grade, old)) gradesMap.put(grade.course, grade)
    }
    gradesMap
  }

  override def filter(grades: Iterable[CourseGrade]): Iterable[CourseGrade] = {
    val newMap = buildGradeMap(grades)
    val groups = AlternativeGradeFilter.analysis(alternativeCourses, newMap)
    for (group <- groups) {
      if (group.grades.exists(_.fullGrade)) {
        // 只把最后一个作为保留，其他删除
        val groupGrades = group.grades.sorted
        val last = groupGrades.remove(groupGrades.size - 1)
        for (sgg <- groupGrades if sgg.fullGrade; course <- sgg.courses) {
          if !last.courses.contains(course) then newMap.remove(course)
        }
      }
    }
    newMap.values
  }
}
