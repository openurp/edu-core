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
import org.beangle.data.orm.MappingMacro.target
import org.openurp.base.edu.model.Course
import org.openurp.code.edu.model.TeachingNature
import org.openurp.code.service.CodeService
import org.openurp.edu.program.util.PlanGroupStat
import org.openurp.edu.program.model.*
import org.openurp.edu.program.service.PlanDiff.*
import org.openurp.edu.program.service.{CoursePlanService, PlanDiff, SharePlanService}

import java.time.Instant
import scala.collection.mutable

class SharePlanServiceImpl extends SharePlanService {

  var entityDao: EntityDao = _
  var codeService: CodeService = _

  override def move(node: ShareCourseGroup, location: ShareCourseGroup, index: Int): Unit = {
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

  override def addCourseGroupToPlan(group: ShareCourseGroup, parent: ShareCourseGroup, plan: SharePlan): Unit = {
    plan.addGroup(group, Option(parent))
    entityDao.saveOrUpdate(group)
    entityDao.saveOrUpdate(plan)
  }

  private def addCourse(planCourse: SharePlanCourse, group: ShareCourseGroup): Unit = {
    group.planCourses.addOne(planCourse)
  }

  override def addPlanCourse(pc: SharePlanCourse, group: ShareCourseGroup): Unit = {
    pc.course = entityDao.get(classOf[Course], pc.course.id)
    pc.group = group
    addCourse(pc, group)
    entityDao.saveOrUpdate(group)
  }

  override def removePlanCourse(planCourse: SharePlanCourse, group: ShareCourseGroup): Unit = {
    group.planCourses.subtractOne(planCourse)
    entityDao.saveOrUpdate(group)
  }

  override def updatePlanCourse(planCourse: SharePlanCourse, group: ShareCourseGroup): Unit = {
    addCourse(planCourse, group)
    entityDao.saveOrUpdate(group)
  }

  private def shiftCode(node: ShareCourseGroup, newParent: ShareCourseGroup, index2: Int): Unit = {
    val sibling =
      if (null != newParent) newParent.children.toBuffer.sorted
      else node.plan.topGroups.toBuffer.sorted
    sibling.subtractOne(node)

    var index = index2
    index -= 1
    if (index > sibling.size) index = sibling.size
    sibling.insert(index, node)
    val nolength = String.valueOf(sibling.size).length
    val nodes = Collections.newSet[ShareCourseGroup]
    for (seqno <- 1 to sibling.size) {
      val one = sibling(seqno - 1)
      generateCode(one, Strings.leftPad(String.valueOf(seqno), nolength, '0'), nodes)
    }
    entityDao.saveOrUpdate(nodes)
    entityDao.refresh(node)
    entityDao.refresh(node.plan)
  }

  def genIndexno(group: ShareCourseGroup, indexno: String): Unit = {
    val newIndexno =
      if (group.parent.isEmpty) indexno
      else if (Strings.isEmpty(indexno)) Strings.concat(group.parent.get.indexno, ".", String.valueOf(index(group)))
      else Strings.concat(group.parent.get.indexno, ".", indexno)
    Properties.set(group, "indexno", newIndexno)
  }

  private def generateCode(node: ShareCourseGroup, indexno: String, nodes: mutable.Set[ShareCourseGroup]): Unit = {
    if (!nodes.contains(node)) {
      nodes.add(node)
      if (null != indexno) genIndexno(node, indexno)
      else genIndexno(node, null)
      node.children foreach { c =>
        generateCode(c, null, nodes)
      }
    }
  }

  private def index(group: ShareCourseGroup): Int = {
    var index = Strings.substringAfterLast(group.indexno, ".")
    if (Strings.isEmpty(index)) index = group.indexno
    var idx = Numbers.toInt(index)
    if (idx <= 0) idx = 1
    idx
  }

}
