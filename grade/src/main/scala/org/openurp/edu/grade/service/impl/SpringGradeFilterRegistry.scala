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

package org.openurp.edu.grade.service.impl

import org.beangle.commons.bean.Initializing
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.edu.grade.domain.GradeFilter
import org.springframework.context.{ApplicationContext, ApplicationContextAware}

/**
 * 基于spring的过滤器注册表
 *
 */
class SpringGradeFilterRegistry extends GradeFilterRegistry with ApplicationContextAware with Initializing {

  val filters = Collections.newMap[String, GradeFilter]

  var context: ApplicationContext = _

  override def init(): Unit = {
    if (null == context) return
    val names = context.getBeanNamesForType(classOf[GradeFilter])
    if (null != names && names.nonEmpty) {
      for (name <- names) {
        filters.put(name, context.getBean(name).asInstanceOf[GradeFilter])
      }
    }
  }

  override def setApplicationContext(context: ApplicationContext): Unit = {
    this.context = context
  }

  def getFilter(name: String): GradeFilter = {
    filters.get(name).orNull
  }

  def getFilters(name: String): Seq[GradeFilter] = {
    if (Strings.isBlank(name)) return List.empty
    val filterNames = Strings.split(name, Array('|', ','))
    filterNames.map(x => filters.get(x)).flatten.toSeq
  }
}
