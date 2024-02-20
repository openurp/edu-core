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
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.code.edu.model.CourseType
import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.grade.domain.{PlanAuditContext, PlanAuditListener}
import org.openurp.edu.grade.model.{CourseAuditResult, CourseGrade, Grade, GroupAuditResult}
import org.openurp.edu.grade.service.impl.PlanAuditCourseTakerListener.*
import org.openurp.edu.program.model.{CourseGroup, PlanCourse}

import java.time.LocalDate
import scala.collection.mutable

object PlanAuditCourseTakerListener {

  private val TakeCourse2Types = "takeCourse2Types"

  private val Group2CoursesKey = "group2CoursesKey"
}

class PlanAuditCourseTakerListener extends PlanAuditListener {

  var entityDao: EntityDao = _

  var defaultPassed: Boolean = false

  override def start(context: PlanAuditContext): Boolean = {
    val builder = OqlBuilder.from(classOf[CourseTaker], "ct").where("ct.std=:std", context.std)
    builder.where("not exists(from " + classOf[CourseGrade].getName +
      " cg where cg.semester=ct.clazz.semester and cg.course=ct.clazz.course " +
      "and cg.std=ct.std and cg.status=:status)", Grade.Status.Published)
    builder.where("ct.clazz.semester.endOn >= :now", LocalDate.now)
    builder.select("ct.clazz.course,ct.clazz.courseType")
    val course2Types = Collections.newMap[Course, CourseType]
    for (c <- entityDao.search(builder)) {
      course2Types.put(c.asInstanceOf[Array[Any]](0).asInstanceOf[Course], c.asInstanceOf[Array[Any]](1).asInstanceOf[CourseType])
    }
    context.params.put(TakeCourse2Types, course2Types)
    context.params.put(Group2CoursesKey, Collections.newBuffer[(GroupAuditResult, Course)])
    true
  }

  override def startGroup(context: PlanAuditContext, courseGroup: CourseGroup, groupResult: GroupAuditResult): Boolean = {
    true
  }

  def startCourseAudit(context: PlanAuditContext, groupResult: GroupAuditResult, planCourse: PlanCourse): Boolean = {
    if (context.params.get(TakeCourse2Types).asInstanceOf[collection.Map[Course, CourseType]]
      .contains(planCourse.course)) {
      context.params.get(Group2CoursesKey)
        .asInstanceOf[mutable.Buffer[(GroupAuditResult, Course)]] += Tuple2(groupResult, planCourse.course)
    }
    true
  }

  override def end(context: PlanAuditContext): Unit = {
    val course2Types = context.params.remove(TakeCourse2Types).asInstanceOf[collection.mutable.Map[Course, CourseType]]
    val results = context.params.remove(Group2CoursesKey).asInstanceOf[mutable.Buffer[(GroupAuditResult, Course)]]
    val used = Collections.newSet[GroupAuditResult]
    for (tuple <- results) {
      add2Group(tuple._2, tuple._1)
      course2Types.remove(tuple._2)
      used.add(tuple._1)
    }
    val lastTarget = getTargetGroupResult(context)
    for ((key, value) <- course2Types) {
      val g = context.coursePlan.getGroup(value).orNull
      var gr: GroupAuditResult = null
      if (null == g || g.planCourses.isEmpty) {
        gr = context.result.getGroupResult(value).orNull
      }
      if (null == gr) gr = lastTarget.orNull
      if (null != gr) {
        add2Group(key, gr)
        used.add(gr)
      }
    }
    for (aur <- used) {
      aur.checkPassed(true)
    }
  }

  private def add2Group(course: Course, groupResult: GroupAuditResult): Unit = {
    var existedResult = groupResult.courseResults.find(_.course == course).orNull
    if (existedResult == null) {
      existedResult = new CourseAuditResult()
      existedResult.course = course
      existedResult.passed = defaultPassed
      groupResult.addCourseResult(existedResult)
    } else {
      if (defaultPassed) existedResult.passed = defaultPassed
      existedResult.groupResult.updateCourseResult(existedResult)
    }
    existedResult.remark = Some(existedResult.remark.getOrElse("") + "/在读")
  }

  private def getTargetGroupResult(context: PlanAuditContext): Option[GroupAuditResult] = {
    val result = context.result
    context.coursePlan.program.offsetType.map { electiveType =>
      var groupResult = result.getGroupResult(electiveType).orNull
      if (null == groupResult) {
        val groupRs = new GroupAuditResult()
        groupRs.indexno = "99.99"
        groupRs.courseType = electiveType
        groupRs.name = electiveType.name
        groupRs.subCount = 0.toShort
        groupResult = groupRs
        result.addGroupResult(groupResult)
      }
      groupResult
    }
  }

  def setEntityDao(entityDao: EntityDao): Unit = {
    this.entityDao = entityDao
  }
}
