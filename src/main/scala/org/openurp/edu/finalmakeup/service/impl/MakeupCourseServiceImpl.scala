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

package org.openurp.edu.finalmakeup.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.base.model.{Department, Semester}
import org.openurp.base.std.model.{Squad, Student}
import org.openurp.edu.exam.model.{FinalMakeupCourse, FinalMakeupTaker}
import org.openurp.edu.finalmakeup.service.{MakeupCourseCrnGenerator, MakeupCourseService}
import org.openurp.edu.grade.model.CourseAuditResult

/** 毕业补考服务
 *
 */
class MakeupCourseServiceImpl extends MakeupCourseService {
  var entityDao: EntityDao = _
  var crnGenerator: MakeupCourseCrnGenerator = _

  override def split(makeupCourse: FinalMakeupCourse): Seq[FinalMakeupCourse] = {
    if (Collections.isNotEmpty(makeupCourse.squads)) {
      val newMcs = Collections.newBuffer[FinalMakeupCourse]
      for (squad <- makeupCourse.squads) {
        val newMc = new FinalMakeupCourse()
        newMc.squads += squad
        newMc.course = makeupCourse.course
        newMc.depart = makeupCourse.depart
        newMc.semester = makeupCourse.semester
        for (taker <- makeupCourse.takers) {
          if (squad.id == taker.std.state.map(_.squad.map(_.id)).getOrElse(0)) {
            newMc.takers += new FinalMakeupTaker(newMc, taker.std, taker.courseType)
          }
        }
        newMc.stdCount = newMc.takers.size
        crnGenerator.gen(newMc)
        newMcs += newMc
        entityDao.saveOrUpdate(newMc)
      }
      entityDao.remove(makeupCourse)
      newMcs.toSeq
    } else {
      List(makeupCourse)
    }
  }

  override def addTaker(makeupCourse: FinalMakeupCourse, std: Student): String = {
    val result = getCourseResult(std, makeupCourse.course)
    if (result.isEmpty) {
      "没有不及格成绩，无需补考"
    } else {
      val existed = getTaker(makeupCourse.semester, makeupCourse.course, std)
      if (existed.isDefined) {
        "已经在" + existed.head.makeupCourse.crn + "中,无需重复添加"
      } else {
        doAddTaker(makeupCourse, std, result)
      }
    }
  }

  private def getCourseResult(std: Student, course: Course): Option[CourseAuditResult] = {
    val builder = OqlBuilder.from(classOf[CourseAuditResult], "courseResult")
    builder.where("courseResult.course=:course", course)
    builder.where("courseResult.groupResult.planResult.std=:std", std)
    entityDao.search(builder).headOption
  }

  private def getTaker(semester: Semester, course: Course, std: Student): Option[FinalMakeupTaker] = {
    val query = OqlBuilder.from(classOf[FinalMakeupTaker], "mt")
      .where("mt.makeupCourse.semester=:semester", semester)
      .where("mt.makeupCourse.course=:course", course)
      .where("mt.std=:std", std)
    entityDao.search(query).headOption
  }

  private def doAddTaker(makeupCourse: FinalMakeupCourse, std: Student, result: Option[CourseAuditResult]): String = {
    result foreach { r =>
      val courseType = r.groupResult.courseType
      val take = new FinalMakeupTaker(makeupCourse, std, courseType)
      take.scores = r.scores
      take.remark = r.remark
      makeupCourse.takers += take
      makeupCourse.stdCount += 1
      entityDao.saveOrUpdate(makeupCourse, take)
    }
    ""
  }

  override def addTaker(semester: Semester, course: Course, std: Student): String = {
    val result = getCourseResult(std, course)
    if (result.isEmpty) {
      "没有不及格成绩，无需补考"
    } else {
      val makeupCourse = getOrCreate(semester, course, std.state.get.department, std.state.get.squad)
      val existed = getTaker(makeupCourse.semester, makeupCourse.course, std)
      if (existed.isDefined) {
        "已经在" + existed.head.makeupCourse.crn + "中,无需重复添加"
      } else {
        doAddTaker(makeupCourse, std, result)
      }
    }
  }

  override def getOrCreate(semester: Semester, course: Course, department: Department, squad: Option[Squad]): FinalMakeupCourse = {
    val builder = OqlBuilder.from(classOf[FinalMakeupCourse], "makeupCourse")
    builder.where("makeupCourse.semester = :semester", semester)
    builder.where("makeupCourse.course = :course", course)
    squad match {
      case None => builder.where("size(makeupCourse.squads)=0")
      case Some(s) => builder.where(":squad in elements(makeupCourse.squads)", s)
    }
    val makeupCourses = entityDao.search(builder)
    if (Collections.isEmpty(makeupCourses)) {
      val makeupCourse = new FinalMakeupCourse
      makeupCourse.semester = semester
      makeupCourse.course = course
      makeupCourse.depart = department
      makeupCourse.project = course.project
      squad.foreach(makeupCourse.squads += _)
      crnGenerator.gen(makeupCourse)
      entityDao.saveOrUpdate(makeupCourse)
      makeupCourse
    } else {
      makeupCourses.head
    }
  }

}
