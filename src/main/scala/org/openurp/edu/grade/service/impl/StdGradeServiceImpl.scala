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

import org.beangle.commons.collection.{Collections, Order}
import org.beangle.commons.lang.Numbers
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.base.model.{Department, Project, Semester}
import org.openurp.base.std.model.Student
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.grade.service.StdGradeService

import java.time.LocalDate

class StdGradeServiceImpl extends StdGradeService {

  private var entityDao: EntityDao = _

  def stdByCode(
                 stdCode: String,
                 project: Project,
                 departments: List[Department],
                 entityDao: EntityDao): Student = {
    val query = OqlBuilder.from(classOf[Student], "std")
    query.where("std.user.code=:code", stdCode)
    if (project == null || Collections.isEmpty(departments)) {
      query.where("std is null")
    } else {
      query.where("std.project = :project", project)
      query.where("std.state.department in (:departments)", departments)
    }
    // 缺少权限限制
    val stds = entityDao.search(query)
    if (Collections.isEmpty(stds)) {
      return null
    }
    if (stds.size == 1) {
      stds.head
    } else {
      throw new RuntimeException("数据异常")
    }
  }

  /**
   * 得到录入教学任务时，符合条件的成绩类别
   *
   * @return
   */
  def buildGradeTypeQuery(): OqlBuilder[GradeType] = {
    val query = OqlBuilder.from(classOf[GradeType], "gradeType")
    query.where("gradeType.id not in (:ids)", Array(GradeType.Final))
    query.where(
      "gradeType.beginOn <= :now and (gradeType.endOn is null or gradeType.endOn >= :now)",
      LocalDate.now)
    query.orderBy(Order.parse("gradeType.code asc"))
    query
  }

  /**
   * 得到教学任务的学生选课状态
   *
   * @return
   */
  def getStatus(
                 crn: String,
                 stdId: String,
                 semesterId: String,
                 entityDao: EntityDao): Array[Any] = {
    val query = OqlBuilder.from(classOf[CourseTaker], "taker")
    query.where("taker.clazz.crn = :crn", crn)
    query.where("taker.std.id = :stdId", Numbers.toLong(stdId))
    query.where("not exists(from " + classOf[CourseGrade].getName +
      " grade where grade.std.id = :stdId and grade.clazz.crn=:crn)", Numbers.toLong(stdId), crn)
    query.where("taker.clazz.semester.id = :semesterId", Numbers.toInt(semesterId))
    query.select("taker.clazz.id,taker.clazz.course.code,taker.clazz.course.name,taker.clazz.gradeState.gradingMode.id,taker.clazz.gradeState.gradingMode.name")
    val takers = entityDao.search(query)
    if (Collections.isEmpty(takers)) {
      return null
    }
    if (takers.size == 1) {
      takers.head.asInstanceOf[Array[Any]]
    } else {
      throw new RuntimeException("数据异常")
    }
  }

  /**
   * 判断一个学生在某一学期内某一门课程成绩是否存在
   *
   * @param std      学生
   * @param semester 学年学期
   * @param course   课程
   * @param project  项目
   * @return false 不存在 true 存在
   */
  def checkStdGradeExists(
                           std: Student,
                           semester: Semester,
                           course: Course,
                           project: Project): Boolean = {
    val builder = OqlBuilder.from(classOf[CourseGrade], "courseGrade")
    builder.where("courseGrade.semester = :semester", semester)
    builder.where("courseGrade.project = :project", project)
    builder.where("courseGrade.std = :student", std)
    builder.where("courseGrade.course = :course", course)
    entityDao.search(builder).nonEmpty
  }

  def setEntityDao(entityDao: EntityDao): Unit = {
    this.entityDao = entityDao
  }
}
