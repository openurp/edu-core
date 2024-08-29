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
import org.beangle.commons.logging.Logging
import org.openurp.base.edu.model.{Course, CourseJournal, Terms}
import org.openurp.code.edu.model
import org.openurp.code.edu.model.{CourseRank, CourseType, TeachingNature}
import org.openurp.edu.program.model.{CourseGroup, CoursePlan}

import javax.script.ScriptEngineManager
import scala.collection.mutable

/**
 * 培养方案学分、学时统计
 */
object PlanCategoryStat {

  def main(args: Array[String]): Unit = {
    val scriptEngineManager = new ScriptEngineManager()
    val scriptEngine = scriptEngineManager.getEngineByName("scala")

    val scalaScript = "val message = \"Hello, Scala!\"\n" + "println(message)"

    scriptEngine.eval(scalaScript)
  }

  def stat(plan: CoursePlan, natures: collection.Seq[TeachingNature]): PlanCategoryStat = {
    val defaultNature = natures.find(_.id == 1).get

    val stat = new PlanCategoryStat(plan, plan.credits, natures)
    val groups = Collections.newBuffer[CourseGroup]
    //收集顶层可以开始统计的课程组
    for (tg <- plan.topGroups) {
      collectGroups(tg, groups)
    }
    for (group <- groups) {
      if (group.optional) {
        val theory = stat.getOrCreateCategory(group, false, false, false, stat.maxterm)
        val practical = stat.getOrCreateCategory(group, false, true, false, stat.maxterm)
        stat.statOptionalGroup(group, theory, practical)
      }
      else stat.statCompulsoryGroup(group)
    }
    stat
  }

  private def collectGroups(group: CourseGroup, results: mutable.Buffer[CourseGroup]): Unit = {
    if (group.rank.nonEmpty) {
      results.addOne(group)
    } else {
      group.children foreach { g =>
        collectGroups(g, results)
      }
    }
  }
}

class PlanCategoryStat(plan: CoursePlan, val credits: Float, natures: collection.Seq[TeachingNature]) extends Logging {
  var lectureNature: TeachingNature = natures.find(_.id == 1).orNull
  var practicalNature: TeachingNature = natures.find(_.id == 9).orNull
  var maxterm = plan.program.endTerm
  var hasOptional = false
  val categoryStats = Collections.newBuffer[CategoryStat]

  def statOptionalGroup(group: CourseGroup, theory: CategoryStat, practical: CategoryStat): Unit = {
    this.hasOptional = true //该计划包含选修课
    val ghours = group.getHours(natures)
    ghours foreach { case (n, hours) =>
      if (group.courseType.practical) {
        if (n.id == TeachingNature.Practice) {
          practical.addGroup(group.credits, Map(practicalNature -> hours), group.termCredits)
        } else {
          theory.addCourse(0f, Map(lectureNature -> hours), Terms.empty)
        }
      } else {
        if (n.id == TeachingNature.Theory) {
          theory.addGroup(group.credits, Map(lectureNature -> hours), group.termCredits)
        } else {
          practical.addCourse(0f, Map(practicalNature -> hours), Terms.empty)
        }
      }
    }
  }

  def isPurePractical(course: Course, courseJournal: CourseJournal, courseType: CourseType): Option[Boolean] = {
    if (courseJournal.hours.isEmpty) {
      Some(course.practical || courseType.module.exists(_.practical))
    } else {
      val hours = courseJournal.hours.filter(_.creditHours > 0)
      if (hours.size == 1) {
        val h = hours.head
        Some(h.nature.id == practicalNature.id)
      } else {
        None
      }
    }
  }

  private def statCompulsoryGroup(g: CourseGroup): Unit = {
    val theory = getOrCreateCategory(g, true, false, false, maxterm) //必修-纯理论课程
    val innerTheory = getOrCreateCategory(g, true, false, true, maxterm) //必修-包含课内理论学时
    val practical = getOrCreateCategory(g, true, true, false, maxterm) //必修-纯实践课
    val innerPractical = getOrCreateCategory(g, true, true, true, maxterm) //必修-包含课内实践学时
    for (pc <- g.orderedPlanCourses) {
      val c = pc.course
      val cj = c.getJournal(plan.program.grade)
      isPurePractical(c, cj, g.courseType) match {
        case Some(pp) =>
          if (pp) {
            practical.addCourse(c.defaultCredits, Map(practicalNature -> cj.creditHours), pc.terms)
          } else {
            theory.addCourse(c.defaultCredits, Map(lectureNature -> cj.creditHours), pc.terms)
          }
        case None =>
          cj.hours foreach { h =>
            if (c.practical || g.courseType.module.nonEmpty && g.courseType.module.get.practical) { //实践课
              if (h.nature.id == practicalNature.id) {
                practical.addCourse(c.defaultCredits, Map(practicalNature -> h.creditHours), pc.terms)
              } else {
                innerTheory.addCourse(0f, Map(lectureNature -> h.creditHours), Terms.empty)
              }
            } else { //理论课
              if (h.nature.id == lectureNature.id) {
                theory.addCourse(c.defaultCredits, Map(lectureNature -> h.creditHours), pc.terms)
              } else {
                innerPractical.addCourse(0f, Map(practicalNature -> h.creditHours), Terms.empty)
              }
            }
          }
      }
    }
    for (child <- g.children) {
      if (child.optional) {
        this.statOptionalGroup(child, theory, practical) //将该组作为一门课程来统计
      } else {
        statCompulsoryGroup(child)
      }
    }
  }

  private def getOrCreateCategory(group: CourseGroup, compulsory: Boolean, practical: Boolean, inner: Boolean, maxTerm: Int): CategoryStat = {
    for (s <- categoryStats) {
      if (s.name == group.name && s.compulsory == compulsory && s.practical == practical && s.inner == inner) return s
    }
    val newer = new CategoryStat(group.indexno + " " + group.name, group.rank.get, compulsory, practical, inner, maxTerm)
    this.categoryStats.addOne(newer)
    newer
  }

  /**
   * 查询选修统计
   *
   * @return
   */
  def optionalStat: CategoryStat = {
    val results = Collections.newBuffer[CategoryStat]
    for (cs <- categoryStats) {
      if (!cs.compulsory) results.addOne(cs)
    }
    merge(results)
  }

  def allStat: CategoryStat = merge(this.categoryStats)

  /**
   * 查询理论环节
   *
   * @return
   */
  def theoreticalStat: CategoryStat = {
    val results = Collections.newBuffer[CategoryStat]
    for (cs <- categoryStats) {
      if (!cs.practical) results.addOne(cs)
    }
    merge(results)
  }

  /**
   * 查询实践环节
   *
   * @return
   */
  def practicalStat: CategoryStat = {
    val results = Collections.newBuffer[CategoryStat]
    for (cs <- categoryStats) {
      if (cs.practical) {
        results.addOne(cs)
      }
    }
    merge(results)
  }

  def practicalCredits: Double = {
    var total = 0d
    var innerHours = 0
    for (cs <- categoryStats.sortBy(_.name)) {
      if (cs.practical) {
        if (cs.credits > 0) total += cs.credits
        else {
          innerHours += cs.hours
        }
      }
    }
    println(s"total:${total},转算：${innerHours},转算学分:${innerHours / 16.0}")
    val c = total + innerHours / 16.0
    if (c % 1 >= 0.5) {
      if c % 1 >= 0.7 then c.intValue + 1 else c.intValue + 0.5
    } else c.intValue
  }

  def getCompulsoryStat(containsPractical: Boolean): CategoryStat = {
    val results = Collections.newBuffer[CategoryStat]
    for (cs <- categoryStats) {
      if (cs.compulsory) {
        if (cs.practical) {
          if containsPractical then results.addOne(cs)
        } else {
          results.addOne(cs)
        }
      }
    }
    merge(results)
  }

  private def merge(results: collection.Seq[CategoryStat]): CategoryStat = {
    if (results.isEmpty) return new CategoryStat("合计", null, false, false, false, 0)
    val head = results.head
    if (results.size == 1) return head
    val target = new CategoryStat(head)
    for (i <- 1 until results.size) {
      target.merge(results(i))
    }
    target
  }

  def designatedSelectiveStat: CategoryStat = {
    val results = Collections.newBuffer[CategoryStat]
    for (cs <- categoryStats) {
      cs.rank foreach { r =>
        if (r.id == CourseRank.DesignatedSelective) {
          results.addOne(cs)
        }
      }
    }
    merge(results)
  }

  def freeSelectiveStat: CategoryStat = {
    val results = Collections.newBuffer[CategoryStat]
    for (cs <- categoryStats) {
      cs.rank foreach { r =>
        if (r.id == CourseRank.FreeSelective) {
          results.addOne(cs)
        }
      }
    }
    merge(results)
  }

  def isHasOptional: Boolean = hasOptional

  def isHasPractice: Boolean = practicalStat.credits > 0
}
