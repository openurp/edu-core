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

import org.openurp.edu.program.model.{MajorPlan, Program, ProgramDoc}

/** 培养方案检查器
 */
trait ProgramChecker {

  def check(program: Program): Seq[String]
}

trait PlanChecker {
  def check(plan: MajorPlan): Seq[String]

  def suitable(program: Program): Boolean = {
    true
  }
}

trait DocChecker {
  def check(doc: ProgramDoc, plan: MajorPlan): Seq[String]
}
