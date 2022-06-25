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

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.OqlBuilder
import org.openurp.base.model.Semester
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.BaseServiceImpl
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.model.Grade

import scala.collection.mutable

class BestCourseGradeProviderImpl extends BaseServiceImpl with CourseGradeProvider {

  var bestGradeFilter: BestGradeFilter = _

  def getPublished(std: Student, semesters: Semester*): collection.Seq[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    query.where("grade.status =:status", Grade.Status.Published)
    if (null != semesters && semesters.nonEmpty) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    query.orderBy("grade.semester.beginOn")
    bestGradeFilter.filter(entityDao.search(query)).toSeq
  }

  def getAll(std: Student, semesters: Semester*): collection.Seq[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    if (null != semesters && semesters.nonEmpty) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    query.orderBy("grade.semester.beginOn")
    bestGradeFilter.filter(entityDao.search(query)).toSeq
  }

  def getPublished(stds: Iterable[Student], semesters: Semester*): collection.Map[Student, collection.Seq[CourseGrade]] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std in (:stds)", stds)
    query.where("grade.status =:status", Grade.Status.Published)
    if (null != semesters && semesters.nonEmpty) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    val allGrades = entityDao.search(query)
    val gradeMap = Collections.newMap[Student, mutable.Buffer[CourseGrade]]
    for (std <- stds) {
      gradeMap.put(std, Collections.newBuffer[CourseGrade])
    }
    for (g <- allGrades) gradeMap(g.std) += g
    stds.map(s => (s, bestGradeFilter.filter(gradeMap(s)).toSeq)).toMap
  }

  def getAll(stds: Iterable[Student], semesters: Semester*): collection.Map[Student, collection.Seq[CourseGrade]] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std in (:stds)", stds)
    if (null != semesters && semesters.nonEmpty) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    val allGrades = entityDao.search(query)
    val gradeMap = Collections.newMap[Student, mutable.Buffer[CourseGrade]]
    for (std <- stds) {
      gradeMap.put(std, Collections.newBuffer[CourseGrade])
    }
    for (g <- allGrades) gradeMap(g.std) += g
    stds.map(s => (s, bestGradeFilter.filter(gradeMap(s)).toSeq)).toMap
  }

  def getPassedStatus(std: Student): collection.Map[Long, Boolean] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "cg")
    query.where("cg.std = :std", std)
    query.select("cg.course.id,cg.passed")
    val rs = entityDao.search(query).asInstanceOf[List[Array[Any]]]
    val courseMap = Collections.newMap[Long, Boolean]
    for (obj <- rs) {
      val courseId = obj(0).asInstanceOf[Number].longValue()
      if (null != obj(1)) {
        if (!courseMap.contains(courseId) || !courseMap(courseId)) {
          courseMap.put(courseId, obj(1).asInstanceOf[java.lang.Boolean].booleanValue)
        }
      } else {
        courseMap.put(courseId, false)
      }
    }
    courseMap
  }

}
