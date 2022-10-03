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

package org.openurp.edu.extern.service.signup

import org.openurp.base.std.model.Student
import org.openurp.edu.extern.config.CertSignupSetting

object CertSignupChecker {
  /** 没有完成要求的上级考试要求 */
  val notPassSuperCategory = "error.extern.exam.notPassSuperCategory"
  /** 重复报名 */
  val existExamSignup = "error.extern.exam.existExamSignup"
  /** 不再报名时间 */
  val notInTime = "error.extern.exam.notInTime"
  /** 通过的不能报名了 */
  val hasPassed = "error.extern.exam.isHasPassed"
}

trait CertSignupChecker {
  def check(student: Student, setting: CertSignupSetting): String
}
