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
import org.beangle.event.bus.{DataEvent, DataEventBus}
import org.openurp.base.edu.model.Course
import org.openurp.base.model.{Campus, Project, Semester}
import org.openurp.base.service.ProjectConfigService
import org.openurp.base.std.model.{Squad, Student}
import org.openurp.code.edu.model.{CourseTakeType, GradeType}
import org.openurp.edu.clazz.model.Clazz
import org.openurp.edu.exam.model.{FinalMakeupCourse, FinalMakeupTaker}
import org.openurp.edu.finalmakeup.service.{FinalMakeupService, MakeupCourseCrnGenerator}
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.model.{AuditCourseResult, CourseGrade, Grade}
import org.openurp.edu.service.Features

import java.time.Instant

/** 毕业补考服务
 *
 */
class FinalMakeupServiceImpl extends FinalMakeupService {
  var entityDao: EntityDao = _
  var crnGenerator: MakeupCourseCrnGenerator = _
  var gradeProvider: CourseGradeProvider = _
  var configService: ProjectConfigService = _
  var eventbus: DataEventBus = _

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
          if (squad.id == taker.std.state.map(_.squad.map(_.id)).getOrElse(0L)) {
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

  private def getGrades(std: Student, course: Course, semester: Semester): collection.Seq[CourseGrade] = {
    val grades = gradeProvider.get(std)
    grades.filter(g => g.course == course && g.semester.beginOn.isBefore(semester.beginOn))
  }

  private def getTaker(course: Course, std: Student): Option[FinalMakeupTaker] = {
    val query = OqlBuilder.from(classOf[FinalMakeupTaker], "mt")
      .where("mt.course=:course", course)
      .where("mt.std=:std", std)
    entityDao.search(query).headOption
  }

  private def doAddTaker(makeupCourse: FinalMakeupCourse, std: Student, result: Option[AuditCourseResult]): String = {
    result foreach { r =>
      val courseType = r.groupResult.courseType
      val take = new FinalMakeupTaker(makeupCourse, std, courseType)
      take.failScores = r.scores
      take.remark = r.remark
      makeupCourse.takers += take
      makeupCourse.stdCount += 1
      entityDao.saveOrUpdate(makeupCourse, take)
    }
    ""
  }

  override def addTaker(std: Student, course: Course, semester: Semester, mc: Option[FinalMakeupCourse]): String = {
    val result = getGrades(std, course, semester)
    if (result.isEmpty) {
      "没有不及格成绩，无需补考"
    } else {
      val existed = getTaker(course, std)
      if (existed.isDefined) {
        "已经在" + existed.flatMap(_.makeupCourse.map(_.crn)).getOrElse("--") + "中,无需重复添加"
      } else {
        createTaker(std, course, semester)
        ""
      }
    }
  }

  override def update(taker: FinalMakeupTaker): Boolean = {
    val grades = getGrades(taker.std, taker.course, taker.semester)
    if (grades.isEmpty) {
      false
    } else {
      val endGaType = new GradeType(GradeType.EndGa)
      //当时的不及格总评成绩，这个通过的成绩有可能是后面补考登记上去的，登记到原始学期上去了
      val failList: collection.Seq[Grade] = grades.sortBy(_.semester.beginOn).map { x =>
        if x.passed then x.getGaGrade(endGaType).getOrElse(x) else x
      }
      val last = grades.last

      taker.courseType = last.courseType
      last.clazz.foreach { clz =>
        taker.teacher = clz.teachers.headOption
        taker.remark = Some(clz.crn)
      }
      taker.failScores = failList.map(_.scoreText.getOrElse("--")).mkString(",")
      taker.updatedAt = Instant.now
      entityDao.saveOrUpdate(taker)
      true
    }
  }

  override def createTaker(std: Student, course: Course, semester: Semester): FinalMakeupTaker = {
    val taker = new FinalMakeupTaker()
    taker.std = std
    taker.course = course
    taker.semester = semester
    update(taker)
    taker
  }

  override def getOrCreate(semester: Semester, course: Course, campus: Campus, squad: Option[Squad]): FinalMakeupCourse = {
    val builder = OqlBuilder.from(classOf[FinalMakeupCourse], "makeupCourse")
    builder.where("makeupCourse.semester = :semester", semester)
    builder.where("makeupCourse.course = :course", course)
    builder.where("makeupCourse.campus = :campus", campus)
    //    squad match {
    //      case None => builder.where("size(makeupCourse.squads)=0")
    //      case Some(s) => builder.where(":squad in elements(makeupCourse.squads)", s)
    //    }
    val makeupCourses = entityDao.search(builder)
    if (Collections.isEmpty(makeupCourses)) {
      val makeupCourse = new FinalMakeupCourse
      makeupCourse.semester = semester
      makeupCourse.course = course
      makeupCourse.campus = campus
      makeupCourse.depart = course.department
      makeupCourse.project = course.project
      squad.foreach(makeupCourse.squads += _)
      crnGenerator.gen(makeupCourse)
      entityDao.saveOrUpdate(makeupCourse)
      makeupCourse
    } else {
      val first = makeupCourses.head
      squad.foreach(first.squads += _)
      first
    }
  }

  /**
   * 查找符合毕业补考的成绩
   */
  override def findFailed(project: Project, semester: Semester): Seq[CourseGrade] = {
    //为防止学期数据不准确，可能结束太早，为毕业生延期一个月做日期判断
    val semesterEndOn = semester.endOn.plusMonths(1)

    //查找上个学期
    val sq = OqlBuilder.from(classOf[Semester], "s")
    sq.where("s.calendar=:calendar and s.beginOn <:beginOn", semester.calendar, semester.beginOn)
    sq.orderBy("s.beginOn desc")
    val next2LastSemester = entityDao.topN(1, sq).headOption

    val builder = OqlBuilder.from[CourseGrade](classOf[AuditCourseResult].getName, "courseResult")
    builder.newFrom(s"${classOf[AuditCourseResult].getName} courseResult,${classOf[CourseGrade].getName} grade,${classOf[Student].getName} std2")
    builder.select("grade")
    builder.where("courseResult.passed=false")
    builder.where("courseResult.course.hasMakeup=true")
    builder.where("courseResult.groupResult.planResult.std=std2")
    builder.where("grade.std=std2 and grade.course=courseResult.course")

    //该成绩是最后一条
    builder.where(s"not exists(from ${classOf[CourseGrade].getName} g2 where g2.std=grade.std and g2.course=grade.course" +
      s" and g2.semester.beginOn > grade.semester.beginOn)")

    builder.where("grade.clazz is not null")

    val scope = configService.getString(project, Features.Finalmakeup.scope)
    //这个成绩不是重修
    if (scope.contains("no-retake")) {
      builder.where("grade.courseTakeType.id != :retaker", CourseTakeType.Repeat)
    }
    //集中在上个学期的不及格成绩
    if (scope.contains("next2last")) {
      next2LastSemester foreach { s => builder.where("grade.semester=:next2last", s) }
    }
    //该课程之后再也没开过,截止到毕业学期
    if (scope.contains("without-clazz")) {
      builder.where(s"not exists(from ${classOf[Clazz].getName} clazz where clazz.course = grade.course" +
        s" and clazz.semester.beginOn >grade.semester.beginOn and clazz.semester.beginOn <:semesterEndOn)", semesterEndOn)
    }
    //不限定是否是应届毕业，有业务人员判断
    //    builder.where("std2.graduateOn =:graduateOn", batch.graduateOn)
    //是毕业生

    builder.where("std2.beginOn < :semesterEndOn and :semesterBeginOn < std2.endOn", semesterEndOn, semester.beginOn)
    builder.where("std2.graduateOn <= :semesterEndOn", semesterEndOn)

    //不存在已有的数据
    val hql = new StringBuilder
    hql.append("not exists (")
    hql.append("  from ").append(classOf[FinalMakeupTaker].getName).append(" taker")
    hql.append(" where taker.semester.id=" + semester.id)
    hql.append("   and taker.course = courseResult.course")
    hql.append("   and taker.std = std2")
    hql.append(")")
    builder.where(hql.toString)
    builder.orderBy("grade.course.code")
    entityDao.search(builder)
  }

  override def saveGrades(makeupCourse: FinalMakeupCourse, grades: Iterable[CourseGrade]): Unit = {
    makeupCourse.inputAt = Some(Instant.now)
    entityDao.saveOrUpdate(makeupCourse, makeupCourse.takers, grades)
    eventbus.publish(DataEvent.update(grades))
  }

  override def removeGrades(makeupCourse: FinalMakeupCourse): Option[String] = {
    if (makeupCourse.status >= Grade.Status.Confirmed) {
      Some("已经提交的成绩不能删除")
    } else {
      makeupCourse.status = 0
      makeupCourse.inputAt = None
      makeupCourse.takers foreach { t => t.score = None }
      val builder = OqlBuilder.from(classOf[CourseGrade], "courseGrade")
      builder.where("courseGrade.crn = :crn", makeupCourse.crn)
      builder.where("courseGrade.course = :course", makeupCourse.course)
      builder.where("courseGrade.semester = :semester", makeupCourse.semester)
      builder.where("courseGrade.clazz is null")
      val grades = entityDao.search(builder)
      entityDao.remove(grades)
      entityDao.saveOrUpdate(makeupCourse, makeupCourse.takers)
      eventbus.publish(DataEvent.remove(grades))
      None
    }
  }
}
