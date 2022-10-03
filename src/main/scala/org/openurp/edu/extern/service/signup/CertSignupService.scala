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

package org.openurp.edu.extern.service.signup;

import org.openurp.base.model.{Project, Semester}
import org.openurp.base.std.model.Student
import org.openurp.edu.extern.code.CertificateSubject
import org.openurp.edu.extern.config.{CertSignupConfig, CertSignupSetting}
import org.openurp.edu.extern.model.CertSignup

import java.time.LocalDate

trait CertSignupService {
  /**
   * 报名(成功时，保存报名记录)<br>
   * 1.检查报名时间是否合适<br>
   * 2.检查如果在报名约束中说明应该完成的其他考试，则检查成绩。<br>
   * 除非该学生的类别处在免考虑的学生类别范围内。<br>
   * 3.不能重复报名<br>
   * 4.在同一时间段内不能同时报两种以上(含)的考试类别 --（5.不能报已经通过的科目,但是六级可以）
   *
   * @param signup
   * @return 返回""如果成功,否则返回错误信息
   */
  def signup(signup: CertSignup, setting: CertSignupSetting): String

  /**
   * 取消报名<br>
   * 1.检查报名时间是否合适
   *
   * @param std
   * @param setting
   * @return
   */
  def cancel(std: Student, setting: CertSignupSetting): String

  /**
   * 查询在特定设置条件下的报名记录
   *
   * @param std
   * @param setting
   * @return
   */
  def get(std: Student, setting: CertSignupSetting): Option[CertSignup]

  /**
   * 获得学生这次期号中的报名记录
   *
   * @param std
   * @param config
   * @return
   */
  def search(std: Student, config: CertSignupConfig): Iterable[CertSignup]

  /**
   * 查询在一定时间段内的学生的报名记录
   *
   * @param std
   * @param start
   * @param end
   * @return
   */
  def search(std: Student, start: LocalDate, end: LocalDate): Iterable[CertSignup]

  /**
   * 判断该学生是否可以报名（true可以报名，false则否
   *
   * @param student
   * @param setting
   * @return
   */
  def canSignup(student: Student, setting: CertSignupSetting): String

  /**
   * 返回现在开放，并且在时间内的设置
   *
   * @return
   */
  def getOpenedSettings(project: Project): Iterable[CertSignupSetting]

  /**
   * 获得这次期号中某门科目开放的期号
   *
   * @return
   */
  def getOpenedConfigs(project: Project): Iterable[CertSignupConfig]

  def isExist(signup: CertSignup): Boolean
}
