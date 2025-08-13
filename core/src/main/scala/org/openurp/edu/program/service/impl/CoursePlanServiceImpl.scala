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

package org.openurp.edu.program.service.impl

import org.beangle.commons.bean.Properties
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.{Numbers, Objects, Strings}
import org.beangle.data.dao.EntityDao
import org.openurp.base.edu.model.{Course, Terms}
import org.openurp.code.edu.model.TeachingNature
import org.openurp.code.service.CodeService
import org.openurp.edu.program.util.PlanGroupStat
import org.openurp.edu.program.model.*
import org.openurp.edu.program.service.PlanDiff.*
import org.openurp.edu.program.service.{CoursePlanService, PlanDiff}

import java.time.Instant
import scala.collection.mutable

class CoursePlanServiceImpl extends CoursePlanService {

  var entityDao: EntityDao = _
  var codeService: CodeService = _

  override def move(node: CourseGroup, location: CourseGroup, index: Int): Unit = {
    if (Objects.equals(node.parent.orNull, location)) {
      if Numbers.toInt(node.indexno) != index then shiftCode(node, location, index)
    } else {
      //如果使用对象操作会引发级联删除
      Properties.set(node, "parent", location)
      entityDao.saveOrUpdate(node)
      entityDao.refresh(node)
      entityDao.refresh(location)
      entityDao.refresh(location.plan)
      shiftCode(node, location, index)
    }
  }

  override def statPlanCredits(plan: CoursePlan): Float = {
    entityDao.refresh(plan)
    val stat = PlanGroupStat.stat(plan, codeService.get(classOf[TeachingNature]))
    var updated = false
    stat.updates() foreach { gs =>
      val group = gs.group.asInstanceOf[AbstractCourseGroup]
      group.credits = gs.credits
      group.creditHours = gs.creditHours
      group.hourRatios = gs.hourRatios
      group.termCredits = gs.termCreditString
      group.terms = gs.terms
      updated = true
    }
    plan match
      case mp: AbstractCoursePlan =>
        if mp.credits != stat.credits then updated = true
        mp.credits = stat.credits
        mp.creditHours = stat.creditHours
        mp.hourRatios = CreditHours.toRatios(stat.hours)
        mp.program.credits = plan.credits
      case _ =>

    entityDao.saveOrUpdate(plan, plan.program)
    if (updated) {
      entityDao.evict(plan)
      entityDao.evict(plan.program)
    }
    plan.credits
  }

  override def addCourseGroupToPlan(group: CourseGroup, parent: CourseGroup, plan: CoursePlan): Unit = {
    plan.addGroup(group, Option(parent))
    entityDao.saveOrUpdate(group)
    entityDao.saveOrUpdate(plan)
    statPlanCredits(plan)
  }

  private def addCourse(planCourse: PlanCourse, group: AbstractCourseGroup): Unit = {
    var buf = group.planCourses
    buf.subtractOne(planCourse)
    buf = buf.sorted(PlanCourseOrdering)

    val firstTerm = planCourse.terms.first
    var idx = planCourse.idx.toInt
    if (idx < 0) idx = 0

    if (idx >= buf.size) idx = buf.size + 1
    else if (idx == 0) {
      var i = 0
      while (i < buf.size) {
        val pc = buf(i)
        val myFirst = pc.terms.first
        if (myFirst < firstTerm || myFirst == firstTerm && pc.course.code.compare(planCourse.course.code) < 0) {
          //pass through
        } else {
          idx = (i + 1)
          i = buf.size //break;
        }
        i = i + 1
      }
      if (idx == 0) idx = buf.size + 1
    }
    buf.insert(idx - 1, planCourse)

    var i = 1
    buf.foreach { pc =>
      pc.asInstanceOf[AbstractPlanCourse].idx = i.toShort
      i += 1
    }
    group.planCourses.addOne(planCourse)
  }

  override def addPlanCourse(planCourse: PlanCourse, group: CourseGroup): Unit = {
    val cg = group.asInstanceOf[AbstractCourseGroup]
    val pc = planCourse.asInstanceOf[AbstractPlanCourse]
    pc.course = entityDao.get(classOf[Course], pc.course.id)
    pc.group = cg
    addCourse(pc, cg)
    entityDao.saveOrUpdate(cg)
    statPlanCredits(cg.plan)
  }

  override def removePlanCourse(planCourse: PlanCourse, group: CourseGroup): Unit = {
    val cg = group.asInstanceOf[AbstractCourseGroup]
    cg.planCourses.subtractOne(planCourse)
    entityDao.saveOrUpdate(cg)
    statPlanCredits(cg.plan)
  }

  override def updatePlanCourse(planCourse: PlanCourse, group: CourseGroup): Unit = {
    val cg = group.asInstanceOf[AbstractCourseGroup]
    addCourse(planCourse, cg)
    entityDao.saveOrUpdate(cg)
    this.statPlanCredits(cg.plan)
  }

  private def shiftCode(node: CourseGroup, newParent: CourseGroup, index2: Int): Unit = {
    val sibling =
      if (null != newParent) newParent.children.toBuffer.sorted
      else node.plan.topGroups.toBuffer.sorted
    sibling.subtractOne(node)

    var index = index2
    index -= 1
    if (index > sibling.size) index = sibling.size
    sibling.insert(index, node)
    val nolength = String.valueOf(sibling.size).length
    val nodes = Collections.newSet[CourseGroup]
    for (seqno <- 1 to sibling.size) {
      val one = sibling(seqno - 1)
      generateCode(one, Strings.leftPad(String.valueOf(seqno), nolength, '0'), nodes)
    }
    entityDao.saveOrUpdate(nodes)
    entityDao.refresh(node)
    entityDao.refresh(node.plan)
  }

  def genIndexno(group: CourseGroup, indexno: String): Unit = {
    val newIndexno =
      if (group.parent.isEmpty) indexno
      else if (Strings.isEmpty(indexno)) Strings.concat(group.parent.get.indexno, ".", String.valueOf(index(group)))
      else Strings.concat(group.parent.get.indexno, ".", indexno)
    Properties.set(group, "indexno", newIndexno)
  }

  private def generateCode(node: CourseGroup, indexno: String, nodes: mutable.Set[CourseGroup]): Unit = {
    if (!nodes.contains(node)) {
      nodes.add(node)
      if (null != indexno) genIndexno(node, indexno)
      else genIndexno(node, null)
      node.children foreach { c =>
        generateCode(c, null, nodes)
      }
    }
  }

  private def index(group: CourseGroup): Int = {
    var index = Strings.substringAfterLast(group.indexno, ".")
    if (Strings.isEmpty(index)) index = group.indexno
    var idx = Numbers.toInt(index)
    if (idx <= 0) idx = 1
    idx
  }

  override def diff(left: CoursePlan, right: CoursePlan): Seq[PlanDiff.GroupDiff] = {
    val diffs = Collections.newBuffer[GroupDiff]
    left.groups.sortBy(_.indexno) foreach { g =>
      val matched = right.getGroup(g.name)
      if (matched.isEmpty) {
        diffs.addOne(GroupDiff(g.indexno, g.name, List.empty, Some(PlanDiff.Group(g.credits, g.planCourses.toSeq)), None))
      } else {
        val leftPcs = g.planCourses.map { x => (x.course, x) }.toMap
        val rightPcs = matched.get.planCourses.map { x => (x.course, x) }.toMap
        val leftCourses = leftPcs.keys.toSet -- rightPcs.keys
        val rightCourses = rightPcs.keys.toSet -- leftPcs.keys
        val leftPlanCourses = leftPcs.filter(x => leftCourses.contains(x._1)).values.toBuffer
        val rightPlanCourses = rightPcs.filter(x => rightCourses.contains(x._1)).values.toBuffer
        val commons = leftPcs.keys.toSet.intersect(rightPcs.keys.toSet)
        val commonsPcs = Collections.newBuffer[(PlanCourse, PlanCourse)]
        commons foreach { common =>
          val lpc = leftPcs(common)
          val rpc = rightPcs(common)
          if (lpc.terms.value != rpc.terms.value) {
            commonsPcs.addOne((lpc, rpc))
          }
        }
        if (leftPlanCourses.nonEmpty || rightPlanCourses.nonEmpty) {
          diffs.addOne(GroupDiff(g.indexno, g.name, commonsPcs.toSeq,
            Some(PlanDiff.Group(g.credits, leftPlanCourses.toSeq)), Some(PlanDiff.Group(matched.get.credits, rightPlanCourses.toSeq))))
        }
      }
    }
    right.groups.sortBy(_.indexno) foreach { g =>
      val matched = left.getGroup(g.name)
      if (matched.isEmpty) {
        diffs.addOne(GroupDiff(g.indexno, g.name, List.empty, None, Some(PlanDiff.Group(g.credits, g.planCourses.toSeq))))
      }
    }
    diffs.sortBy(_.indexno).toSeq
  }

  override def getMajorPlans(programs: Iterable[Program]): Map[Program, MajorPlan] = {
    val existsPlans = entityDao.findBy(classOf[MajorPlan], "program", programs).map(x => (x.program, x)).toMap
    val plans = Collections.newBuffer[MajorPlan]
    programs foreach { program =>
      existsPlans get program match {
        case Some(plan) => plans += plan
        case None =>
          val plan = new MajorPlan
          plan.program = program
          plan.updatedAt = Instant.now
          entityDao.saveOrUpdate(plan)
          plans += plan
      }
    }
    plans.map(x => (x.program, x)).toMap
  }

  override def generate(plan: MajorPlan): ExecutivePlan = {
    entityDao.findBy(classOf[ExecutivePlan], "program", plan.program).headOption match
      case None =>
        val ep = new ExecutivePlan(plan)
        entityDao.saveOrUpdate(ep)
        ep
      case Some(ep) => ep
  }

  override def copy(src: MajorPlan, target: MajorPlan): Unit = {
    src.topGroups.foreach { group =>
      new MajorCourseGroup(target, group.asInstanceOf[AbstractCourseGroup])
    }
  }
}
