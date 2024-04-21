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

package org.openurp.edu.grade.service.audit

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.edu.model.Course
import org.openurp.edu.clazz.model.CourseTaker
import org.openurp.edu.grade.domain.{AuditPlanContext, AuditPlanListener}
import org.openurp.edu.grade.model.{AuditCourseResult, AuditGroupResult, CourseGrade, Grade}
import org.openurp.edu.program.domain.AlternativeCourseProvider

class AuditCourseTakerListener extends AuditPlanListener {

  var entityDao: EntityDao = _

  var alternativeCourseProvider: AlternativeCourseProvider = _

  override def end(context: AuditPlanContext): Unit = {
    if (context.result.passed) return

    val builder = OqlBuilder.from(classOf[CourseTaker], "ct").where("ct.std=:std", context.std)
    builder.where(s"not exists(from ${classOf[CourseGrade].getName} cg where cg.semester=ct.clazz.semester" +
      " and cg.course=ct.clazz.course and cg.std=ct.std and cg.status=:status)", Grade.Status.Published)
    val courseTakers = entityDao.search(builder)
    val last = getTargetGroupResult(context)
    val result = context.result
    val used = Collections.newBuffer[CourseTaker]
    courseTakers foreach { ct =>
      result.getCourseResult(ct.course) foreach { cr =>
        used.addOne(ct)
        cr.taking = true
        cr.addRemark("在读")
        cr.groupResult.addCourseResult(cr)
      }
    }

    //剩余一些在读课程，考虑替代
    val reminded = courseTakers.toBuffer.subtractAll(used).map(_.course).toSet
    if (reminded.nonEmpty) {
      val courseMap = Collections.newMap[Course, AuditCourseResult]
      for (groupResult <- context.result.groupResults) {
        for (car <- groupResult.courseResults) {
          if !car.passed && !car.taking && !car.predicted then courseMap.put(car.course, car)
        }
      }
      //如果有课程不及格，开始检查能否替代及格
      if (courseMap.nonEmpty) {
        val substitutions = alternativeCourseProvider.getAlternatives(context.result.std)
        for (sc <- substitutions if sc.olds.subsetOf(courseMap.keySet) && sc.news.subsetOf(reminded)) {
          for (ori <- sc.olds) {
            val cr = courseMap(ori)
            cr.taking = true
            cr.addRemark("在读")
            cr.addRemark(sc.news.map(_.name).mkString(","))
            cr.groupResult.addCourseResult(cr)
            used.addAll(courseTakers.filter(x => sc.news.contains(x.course)))
          }
        }
      }
    }
    //如果找不到直接的审核结果,也不能替代的
    courseTakers.toBuffer.subtractAll(used) foreach { ct =>
      var courseType = ct.courseType
      if (null == courseType) courseType = ct.clazz.courseType
      val target = context.getGroup(ct.course, courseType).flatMap(x => result.getGroupResult(x.name)).orElse(last)
      target foreach { t =>
        used.addOne(ct)
        add2Group(ct, t, last.contains(t))
      }
    }

  }

  private def add2Group(taker: CourseTaker, groupResult: AuditGroupResult, isLast: Boolean): Unit = {
    val cr = groupResult.getCourseResult(taker.course).getOrElse(new AuditCourseResult(taker.course))
    cr.taking = true
    groupResult.addCourseResult(cr)
    cr.addRemark("在读")
    var courseType = taker.courseType
    if (null == courseType) courseType = taker.clazz.courseType
    if (isLast && courseType != groupResult.courseType) {
      cr.addRemark(s"原${courseType.name}")
    }
  }

  /** 获取转换目标课程组审核结果
   *
   * @param context
   * @return
   */
  private def getTargetGroupResult(context: AuditPlanContext): Option[AuditGroupResult] = {
    val result = context.result
    context.coursePlan.program.offsetType.map { electiveType => //FIXME fix user data on offset type
      var groupResult = result.getGroupResult(electiveType.name).orNull
      if (null == groupResult) {
        val groupRs = new AuditGroupResult(electiveType.name, electiveType)
        groupRs.indexno = "99.99"
        groupResult = groupRs
        result.addGroupResult(groupResult)
      }
      groupResult
    }
  }

}
