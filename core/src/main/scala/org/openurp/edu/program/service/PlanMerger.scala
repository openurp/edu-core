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

import org.openurp.edu.program.model.{MajorCourseGroup, MajorPlan, MajorPlanCourse}

object PlanMerger {

  def merge(source: MajorPlan, target: MajorPlan): Unit = {
    source.topGroups foreach { g =>
      target.getGroup(g.name) match
        case None =>
          val mcg = g.asInstanceOf[MajorCourseGroup]
          mcg.follow(target)
          target.addGroup(mcg)
        case Some(tg) =>
          mergeGroup(g.asInstanceOf[MajorCourseGroup], tg.asInstanceOf[MajorCourseGroup])
    }
  }

  private def mergeGroup(source: MajorCourseGroup, target: MajorCourseGroup): Unit = {
    target.credits = source.credits
    target.creditHours = source.creditHours
    target.weeks = source.weeks
    target.terms = source.terms
    target.termCredits = source.termCredits
    target.rank = source.rank
    target.indexno = source.indexno
    target.direction = source.direction
    target.hourRatios = source.hourRatios
    target.required = source.required
    target.stage = source.stage
    target.departments = source.departments
    target.direction = source.direction
    target.remark = source.remark

    val sm = source.planCourses.map(x => (x.course, x)).toMap
    val tm = target.planCourses.map(x => (x.course, x)).toMap
    sm.keySet.diff(tm.keySet) foreach { c =>
      val pc = sm(c)
      pc.asInstanceOf[MajorPlanCourse].group = target
      target.planCourses.addOne(pc)
    }
    tm.keySet.diff(sm.keySet) foreach { c =>
      target.planCourses.subtractOne(tm(c))
    }
    sm.keySet.intersect(tm.keySet) foreach { c =>
      val spc = sm(c).asInstanceOf[MajorPlanCourse]
      val tpc = tm(c).asInstanceOf[MajorPlanCourse]
      tpc.terms = spc.terms
      tpc.termText = spc.termText
      tpc.compulsory = spc.compulsory
      tpc.idx = spc.idx
      tpc.weekstate = spc.weekstate
      tpc.remark = spc.remark
    }

    val sgm = source.children.map(x => (x.name, x)).toMap
    val tgm = target.children.map(x => (x.name, x)).toMap
    sgm.keySet.diff(tgm.keySet) foreach { c =>
      val sg = sgm(c).asInstanceOf[MajorCourseGroup]
      sg.follow(target.plan)
      target.addGroup(sg)
    }
    tgm.keySet.diff(sgm.keySet) foreach { c =>
      val tg = tgm(c).asInstanceOf[MajorCourseGroup]
      target.children.subtractOne(tg)
    }
    sgm.keySet.intersect(tgm.keySet) foreach { c =>
      val sg = sgm(c).asInstanceOf[MajorCourseGroup]
      val tg = tgm(c).asInstanceOf[MajorCourseGroup]
      mergeGroup(sg, tg)
    }
  }
}
