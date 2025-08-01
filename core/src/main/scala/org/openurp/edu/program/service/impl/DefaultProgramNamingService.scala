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

import org.beangle.data.dao.EntityDao
import org.openurp.base.edu.model.{Direction, Major}
import org.openurp.base.std.model.Grade
import org.openurp.edu.program.model.Program
import org.openurp.edu.program.service.ProgramNamingService

import java.text.MessageFormat

/**
 * Program命名帮助类
 */
class DefaultProgramNamingService extends ProgramNamingService {
  var entityDao: EntityDao = _
  /**
   * 培养方案命名格式:专业 方向
   */
  private val NAJOR_NAMING_FMT = "{0} {1}{2}"

  def name(program: Program): String = {
    //fetch database object to avoid entity only contain id.
    val grade = entityDao.get(classOf[Grade], program.grade.id)
    val major = entityDao.get(classOf[Major], program.major.id)
    val direction = program.direction.map(d => entityDao.get(classOf[Direction], d.id))

    val gradeCode = grade.code
    val majorName = major.name
    var directionName = ""
    direction foreach { d => directionName = "/" + d.name }
    MessageFormat.format(NAJOR_NAMING_FMT, gradeCode, majorName, directionName)
  }
}
