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

package org.openurp.edu.clazz.service.impl

import org.beangle.data.dao.EntityDao
import org.beangle.ems.app.EmsApp
import org.beangle.security.Securities
import org.openurp.base.model.User
import org.openurp.edu.clazz.model.*
import org.openurp.edu.clazz.service.ClazzDocService
import org.openurp.edu.textbook.model.ClazzMaterial

import java.io.InputStream
import java.time.Instant

class ClazzDocServiceImpl extends ClazzDocService {

  var entityDao: EntityDao = _

  override def createDoc(clazz: Clazz, name: String, url: Option[String], in: Option[InputStream], fileName: Option[String]): ClazzDoc = {
    val doc = new ClazzDoc
    doc.clazz = clazz
    val user = entityDao.findBy(classOf[User], "code", List(Securities.user)).head
    doc.updatedBy = user
    doc.updatedAt = Instant.now
    doc.name = name
    val blob = EmsApp.getBlobRepository(true)
    in foreach { is =>
      val meta = blob.upload(s"/clazz/${clazz.semester.id}/${clazz.id}/material/", is, fileName.get, user.code + " " + user.name)
      doc.filePath = Some(meta.filePath)
    }
    url foreach { url =>
      doc.url = if (url.startsWith("http")) Some(url) else Some("http://" + url)
    }
    entityDao.saveOrUpdate(doc)
    doc
  }

  override def createNoticeFile(notice: ClazzNotice, is: InputStream, fileName: String): ClazzNoticeFile = {
    val clazz = notice.clazz
    val file = new ClazzNoticeFile
    file.notice = notice
    file.updatedAt = Instant.now
    file.name = fileName
    val user = entityDao.findBy(classOf[User], "code", List(Securities.user)).head
    val blob = EmsApp.getBlobRepository(true)
    val meta = blob.upload(s"/clazz/${clazz.semester.id}/${clazz.id}/notice/", is, fileName, user.code + " " + user.name)
    file.filePath = meta.filePath
    file.mediaType = meta.mediaType
    entityDao.saveOrUpdate(file)
    file
  }

  def createBulletinFile(bulletin: ClazzBulletin, is: InputStream, fileName: String): Unit = {
    val blob = EmsApp.getBlobRepository(true)
    val clazz = bulletin.clazz
    val user = entityDao.findBy(classOf[User], "code", List(Securities.user)).head
    val meta = blob.upload(s"/clazz/${clazz.semester.id}/${clazz.id}/bulletin/", is, fileName, user.code + " " + user.name)
    bulletin.contactQrcodePath = Some(meta.filePath)
    entityDao.saveOrUpdate(bulletin)
  }
}
