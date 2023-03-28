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

package org.openurp.edu.exempt.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Numbers
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.model.AuditStatus
import org.openurp.code.edu.model.ExamStatus
import org.openurp.edu.exempt.model.CertExemptApply
import org.openurp.edu.exempt.service.{CertExemptApplyService, ExemptionService}
import org.openurp.edu.extern.model.CertificateGrade
import org.openurp.edu.program.domain.CoursePlanProvider

import java.time.Instant

class CertExemptApplyServiceImpl extends CertExemptApplyService {

  var entityDao: EntityDao = _

  var exemptionService: ExemptionService = _

  def accept(apply: CertExemptApply): Unit = {
    val grade = convert(apply)
    entityDao.saveOrUpdate(grade)
    exemptionService.addExemption(grade, grade.exempts)
    apply.status = AuditStatus.Passed
    entityDao.saveOrUpdate(apply)
  }

  def reject(apply: CertExemptApply): Unit = {
    val grade = convert(apply)
    if grade.persisted then entityDao.remove(grade) //FIXME 如果是管理员导入的，这种删除有点玄
    grade.exempts foreach { c =>
      exemptionService.removeExemption(grade, c)
    }
    apply.status = AuditStatus.Rejected
    entityDao.saveOrUpdate(apply)
  }

  private def convert(apply: CertExemptApply): CertificateGrade = {
    val query = OqlBuilder.from(classOf[CertificateGrade], "cg")
    query.where("cg.std = :std", apply.std)
    query.where("cg.subject = :subject", apply.subject)
    query.where("cg.acquiredOn = :acquiredOn", apply.acquiredOn)
    val grades = entityDao.search(query)

    val grade = grades.headOption match {
      case None =>
        val g = new CertificateGrade
        g.std = apply.std
        g.subject = apply.subject
        g.acquiredOn = apply.acquiredOn
        g
      case Some(g) => g
    }

    grade.scoreText = apply.scoreText
    if (Numbers.isDigits(apply.scoreText)) {
      grade.score = Some(Numbers.toFloat(apply.scoreText))
    }
    grade.passed = true
    grade.certificate = apply.certificate
    grade.status = 2
    grade.examStatus = new ExamStatus(ExamStatus.Normal)
    grade.exempts ++= apply.courses
    grade.gradingMode = apply.gradingMode
    grade.updatedAt = Instant.now
    grade
  }
}
