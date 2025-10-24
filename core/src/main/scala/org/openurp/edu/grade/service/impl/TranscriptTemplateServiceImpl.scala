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

import org.beangle.data.dao.OqlBuilder
import org.openurp.base.model.Project
import org.openurp.edu.grade.config.TranscriptTemplate
import org.openurp.edu.grade.service.{BaseServiceImpl, TranscriptTemplateService}

class TranscriptTemplateServiceImpl extends BaseServiceImpl, TranscriptTemplateService {

  def getTemplate(project: Project, code: String): TranscriptTemplate = {
    val builder = OqlBuilder.from(classOf[TranscriptTemplate], "rt")
    builder.where("rt.project =:project and rt.code=:code", project, code)
      .cacheable()
    val templates = entityDao.search(builder)
    if ((templates.isEmpty)) null else templates.head
  }

  def getCategoryTemplates(project: Project, category: String): Seq[TranscriptTemplate] = {
    val builder = OqlBuilder.from(classOf[TranscriptTemplate], "rt")
    builder.where("rt.project =:project and rt.category=:category", project, category)
      .cacheable()
    entityDao.search(builder)
  }
}
