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

import org.beangle.commons.lang.Numbers
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.model.AuditStatus
import org.openurp.base.service.SemesterService
import org.openurp.code.edu.model.ExamStatus
import org.openurp.edu.exempt.flow.CertExemptApply
import org.openurp.edu.exempt.service.{CertExemptApplyService, ExemptionService}
import org.openurp.edu.extern.model.CertificateGrade
import org.openurp.edu.grade.model.Grade

import java.time.Instant

class CertExemptApplyServiceImpl extends CertExemptApplyService {

  var entityDao: EntityDao = _

  var exemptionService: ExemptionService = _

  var semesterService: SemesterService = _

  def accept(apply: CertExemptApply): Unit = {
    val grade = convert(apply)
    entityDao.saveOrUpdate(grade)
    exemptionService.addExemption(grade, grade.exempts, exemptionService.calcExemptScore(grade))
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
    query.where("cg.certificate = :certificate", apply.certificate)
    query.where("cg.acquiredIn = :acquiredIn", apply.acquiredIn)
    val grades = entityDao.search(query)

    val grade = grades.headOption match {
      case None =>
        val g = new CertificateGrade
        g.std = apply.std
        g.certificate = apply.certificate
        g.acquiredIn = apply.acquiredIn
        g
      case Some(g) => g
    }

    grade.scoreText = apply.scoreText
    if (Numbers.isDigits(apply.scoreText)) {
      grade.score = Some(Numbers.toFloat(apply.scoreText))
    }
    grade.semester = semesterService.get(apply.std.project, apply.acquiredIn.atDay(1))
    if (grade.semester == null) grade.semester = apply.semester

    grade.passed = true
    grade.certificateNo = apply.certificateNo
    grade.status = Grade.Status.Published
    grade.examStatus = new ExamStatus(ExamStatus.Normal)
    grade.exempts ++= apply.courses
    grade.gradingMode = apply.gradingMode
    grade.updatedAt = Instant.now
    grade
  }
}
