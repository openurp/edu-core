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
import org.openurp.code.edu.model.Certificate
import org.openurp.edu.extern.model.CertificateGrade
import org.openurp.edu.extern.service.CertificateGradeService

class DefaultCertificateGradeService extends CertificateGradeService {
  var entityDao: EntityDao = _

  override def getBest(std: Student, cert: Certificate): CertificateGrade = {
    val builder = OqlBuilder.from(classOf[CertificateGrade], "g")
    builder.where("g.std=:std", std)
    builder.where("g.certificate = :cert", cert)
    builder.where("not exists(from " + classOf[CertificateGrade].getName +
      " g2 where g2.std=g.std and g2.certificate =g.certificate and g2.score > g.score)")
    entityDao.search(builder).headOption.orNull
  }

  override def getPassed(std: Student, certificates: Iterable[Certificate]): List[CertificateGrade] = {
    val builder = OqlBuilder.from(classOf[CertificateGrade], "g")
    builder.where("g.std=:std", std)
    builder.where("g.certificate in (:certificates)", certificates)
    builder.where("g.passed=true")
    entityDao.search(builder).toList
  }

  override def isPass(std: Student, certificate: Certificate): Boolean = {
    val grades = entityDao.findBy(classOf[CertificateGrade], "std" -> std, "certificate" -> certificate)
    grades.exists(_.passed)
  }

  override def get(std: Student, best: Boolean): Iterable[CertificateGrade] = {
    val builder = OqlBuilder.from(classOf[CertificateGrade], "g")
    builder.where("g.std=:std", std)
    if (best) {
      builder.where("not exists(from " + classOf[CertificateGrade].getName +
        " g2 where g2.std=g.std and g2.certificate =g.certificate and g2.score > g.score)")
    }
    entityDao.search(builder)
  }
}
