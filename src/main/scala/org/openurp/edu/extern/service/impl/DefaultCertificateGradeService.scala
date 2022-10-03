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

package org.openurp.edu.extern.service.impl

import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.std.model.Student
import org.openurp.edu.extern.code.{CertificateCategory, CertificateSubject}
import org.openurp.edu.extern.model.CertificateGrade
import org.openurp.edu.extern.service.CertificateGradeService

class DefaultCertificateGradeService extends CertificateGradeService {
  var entityDao: EntityDao = _

  override def getBest(std: Student, category: CertificateCategory): CertificateGrade = {
    val builder = OqlBuilder.from(classOf[CertificateGrade], "g")
    builder.where("g.std=:std", std)
    builder.where("g.subject.category = :category", category)
    builder.where("not exists(from " + classOf[CertificateGrade].getName +
      " g2 where g2.std=g.std and g2.subject =g.subject and g2.score > g.score)")
    entityDao.search(builder).headOption.orNull
  }

  override def getPassed(std: Student, subjects: Iterable[CertificateSubject]): List[CertificateGrade] = {
    val builder = OqlBuilder.from(classOf[CertificateGrade], "g")
    builder.where("g.std=:std", std)
    builder.where("g.subject in (:subjects)", subjects)
    builder.where("g.passed=true")
    entityDao.search(builder).toList
  }

  override def isPass(std: Student, subject: CertificateSubject): Boolean = {
    val grades = entityDao.findBy(classOf[CertificateGrade], "std", List(std))
    grades.filter(_.passed).nonEmpty
  }

  override def get(std: Student, best: Boolean): Iterable[CertificateGrade] = {
    val builder = OqlBuilder.from(classOf[CertificateGrade], "g")
    builder.where("g.std=:std", std)
    if (best) {
      builder.where("not exists(from " + classOf[CertificateGrade].getName +
        " g2 where g2.std=g.std and g2.subject =g.subject and g2.score > g.score)")
    }
    entityDao.search(builder)
  }
}
