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

package org.openurp.edu.program.service

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.base.edu.model.Terms
import org.openurp.code.edu.model.TeachingNature
import org.openurp.edu.program.model.{CourseGroup, CoursePlan, PlanCourse}

/** 统计计划中每个组的学分
 */
object PlanGroupStat {

  def stat(plan: CoursePlan, natures: collection.Seq[TeachingNature]): PlanGroupStat = {
    new PlanGroupStat(plan, natures)
  }
}

/** 统计计划的学分
 */
class PlanGroupStat private(plan: CoursePlan, natures: collection.Seq[TeachingNature]) {

  var credits: Float = 0f
  var creditHours: Int = 0
  var hours = Collections.newMap[TeachingNature, Int]

  private val datas = Collections.newMap[CourseGroup, GroupCredit]

  stat()

  def getGroupCredit(group: CourseGroup): GroupCredit = {
    datas(group)
  }

  private def stat(): Unit = {
    for (group <- plan.topGroups) {
      statGroup(group)
      val gs = datas(group)
      creditHours += group.creditHours
      credits += group.credits
      gs.hours foreach { (nature, creditHours) =>
        hours.put(nature, hours.getOrElseUpdate(nature, 0) + creditHours)
      }
    }
  }

  private def statGroup(group: CourseGroup): Unit = {
    for (child <- group.children) {
      statGroup(child)
    }
    doStat(group)
  }

  private def doStat(group: CourseGroup): Unit = {
    //之所以判断(group.children.nonEmpty|| group.planCourses.nonEmpty) 是为了留一个空的必修组
    //这样组存在可能有意义
    if (group.autoAddup && (group.children.nonEmpty || group.planCourses.nonEmpty)) {
      var credits = 0f
      var creditHours = 0
      val termCredits = Array.ofDim[Float](plan.terms)
      val hours = Collections.newMap[TeachingNature, Int]
      var terms = Terms.empty
      for (child <- group.children) {
        val childStat = datas(child)
        credits += childStat.credits
        creditHours += childStat.creditHours
        childStat.hours foreach { (nature, creditHours) =>
          hours.put(nature, hours.getOrElseUpdate(nature, 0) + creditHours)
        }
        mergeCredits(childStat.termCredits, termCredits)
        terms += childStat.terms
      }
      for (pc <- group.planCourses) {
        val journal = pc.journal
        creditHours += journal.creditHours
        credits += pc.credits

        journal.hours foreach { h =>
          hours.put(h.nature, hours.getOrElseUpdate(h.nature, 0) + h.creditHours)
        }
        terms += pc.terms
        addCredits(pc, termCredits)
      }

      val stat = new GroupCredit(group, credits, creditHours, hours.toMap, termCredits, terms)
      datas.put(group, stat)
    } else {
      val stat = new GroupCredit(group, group.credits, group.creditHours,
        group.getHours(natures), Strings.split(group.termCredits).map(_.toFloat), group.terms)
      datas.put(group, stat)
    }
  }

  private def addCredits(planCourse: PlanCourse, termCredits: Array[Float]): Unit = {
    if (planCourse.terms != null) {
      val termList = planCourse.terms.termList
      if (termList.nonEmpty) {
        val first = termList.head - 1 //学期的描述都是1开始的
        if (first >= 0 && first < termCredits.length) {
          termCredits(first) += planCourse.credits
        }
      }
    }
  }

  private def mergeCredits(src: Array[Float], tar: Array[Float]): Unit = {
    tar.indices foreach { i =>
      tar(i) += src(i)
    }
  }

  def updates(): Seq[GroupCredit] = {
    datas.values.filter(_.hasDiff).toSeq.sortBy(_.group.indexno)
  }

  override def toString: String = {
    val groups = datas.keys.toSeq.sortBy(_.indexno)
    groups.map(x => getGroupCredit(x)).mkString("\n")
  }
}

class GroupCredit(val group: CourseGroup, val credits: Float, val creditHours: Int, val hours: Map[TeachingNature, Int],
                  val termCredits: Array[Float], val terms: Terms) {

  def termCreditString: String = termCredits.mkString(",")

  def hourRatios: String = {
    hours.map(x => s"${x._1.id}:${x._2}").toSeq.sorted.mkString(",")
  }

  def hasDiff: Boolean = {
    group.credits != credits || group.creditHours != creditHours || group.hourRatios != hourRatios ||
      group.termCredits != termCreditString || group.terms != terms

  }

  override def toString: String = {
    s"${group.indexno} ${group.name} ${credits}学分 ${creditHours}学时 ${hourRatios} ${termCredits.mkString(",")}"
  }
}
