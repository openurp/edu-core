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

package org.openurp.edu.program.util

import org.openurp.base.edu.model.Terms
import org.openurp.base.model.CalendarStage
import org.openurp.edu.program.model.{CourseGroup, Executable, PlanCourse}

object TermHelper {

  def getTermText(pc: PlanCourse): String = {
    pc match
      case e: Executable =>
        if e.termText.nonEmpty then e.termText.get
        else toTermText(pc.terms, pc.group.stage)
      case _ => toTermText(pc.terms, pc.group.stage)
  }

  def getTermText(g: CourseGroup): String = {
    toTermText(g.terms, g.stage)
  }

  private def toTermText(terms: Terms, stage: Option[CalendarStage]): String = {
    val suffix = getStagePostfix(stage)
    val termList = terms.termList
    if termList.isEmpty then ""
    else if termList.size == 1 then termList.head + suffix
    else if termList.size == 2 then
      termList.head + suffix + "+" + termList.last + suffix
    else
      val last = termList.last
      val first = termList.head
      if (last - first + 1 == termList.size) then first + suffix + "-" + last + suffix
      else termList.map(x => x + suffix).mkString(",")
  }

  private def getStagePostfix(stage: Option[CalendarStage]): String = {
    stage.map(_.name.substring(0, 1)).getOrElse("")
  }
}
