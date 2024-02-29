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

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.openurp.edu.grade.service.TranscriptDataProvider
import org.springframework.beans.BeansException
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.beangle.commons.bean.Initializing

/**
 * 基于spring的过滤器注册表
 *
 *
 */
class SpringTranscriptDataProviderRegistry extends ApplicationContextAware with Initializing {

  val providers = Collections.newMap[String, TranscriptDataProvider]

  var context: ApplicationContext = _

  override def init(): Unit = {
    if (null == context) return
    val names = context.getBeanNamesForType(classOf[TranscriptDataProvider])
    if (null != names && names.nonEmpty) {
      for (name <- names) {
        providers.put(name, context.getBean(name).asInstanceOf[TranscriptDataProvider])
      }
    }
  }

  def getProvider(name: String): TranscriptDataProvider = {
    providers.get(name).getOrElse(null)
  }

  def getProviders(name: String): Seq[TranscriptDataProvider] = {
    if (Strings.isBlank(name)) return List.empty
    val filterNames = Strings.split(name, Array('|', ','))
    filterNames.map(x => providers.get(x)).flatten.toSeq
  }

  override def setApplicationContext(context: ApplicationContext): Unit = {
    this.context = context
  }
}
