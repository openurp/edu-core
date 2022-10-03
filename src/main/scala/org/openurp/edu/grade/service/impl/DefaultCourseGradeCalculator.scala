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

import java.time.Instant

import org.beangle.data.dao.EntityDao
import org.openurp.code.edu.model.{CourseTakeType, ExamStatus, GradeType, GradingMode}
import org.openurp.base.std.model.Student
import org.openurp.edu.grade.domain.NumRounder
import org.openurp.edu.grade.model.{CourseGrade, CourseGradeState, ExamGrade, GaGrade}
import org.openurp.edu.grade.service.{CourseGradeCalculator, CourseGradeSettings, GradeRateService}
import org.openurp.edu.grade.model.Grade

object DefaultCourseGradeCalculator {

  private val EndGa = new GradeType(GradeType.EndGa)

  private val MakeupGa = new GradeType(GradeType.MakeupGa)

  private val Makeup = new GradeType(GradeType.Makeup)

  private val DelayGa = new GradeType(GradeType.DelayGa)

  private val Delay = new GradeType(GradeType.Delay)

  private val End = new GradeType(GradeType.End)
}

import org.beangle.security.Securities
import org.openurp.edu.grade.service.impl.DefaultCourseGradeCalculator._

class DefaultCourseGradeCalculator extends CourseGradeCalculator {

  var entityDao: EntityDao = _

  var gradeRateService: GradeRateService = _

  var settings: CourseGradeSettings = _

  var minEndScore: Float = 0

  var endIsGaWhenFreeListening: Boolean = true

  var numRounder: NumRounder = NumRounder.Normal

  def calcFinal(grade: CourseGrade, state: CourseGradeState): Unit = {
    if (!grade.published) grade.status = guessFinalStatus(grade)
    updateScore(grade, calcScore(grade, state), null)
  }

  override def calcAll(grade: CourseGrade, state: CourseGradeState): Unit = {
    calcEndGa(grade, state)
    calcMakeupDelayGa(grade, state)
  }

  /** 计算期末总评
   *
   * @param grade
   * @param state
   * @return 总评成绩,但不改动成绩
   */
  override def calcEndGa(grade: CourseGrade, state: CourseGradeState): GaGrade = {
    val stdId = grade.std.id
    grade.std = entityDao.get(classOf[Student], stdId)
    val gag = getGaGrade(grade, EndGa)
    val gaScore = calcEndGaScore(grade, state)
    updateScore(gag, gaScore, null)
    if (!gag.published && null != state) gag.status = state.getStatus(gag.gradeType)
    if (!grade.published) grade.status = guessFinalStatus(grade)
    if (gag.status == grade.status) {
      updateScore(grade, calcScore(grade, state), null)
    }
    gag
  }

  protected def calcEndGaScore(grade: CourseGrade, state: CourseGradeState): Option[Float] = {
    var isCheating = false
    for (eg <- grade.examGrades if !isCheating) {
      if (eg.gradeType != Makeup && eg.gradeType != Delay) {
        if (null != eg.examStatus && eg.examStatus.cheating) {
          isCheating = true
        }
      }
    }
    if (isCheating) return Some(0f)

    var ga: Option[Float] = None
    var gaGrade = grade.getGaGrade(EndGa).orNull
    if (gaGrade != null) {
      ga = gaGrade.score
      if (grade.examGrades.isEmpty) return ga
    }
    val endGrade = grade.getExamGrade(End).orNull
    gaGrade = getGaGrade(grade, EndGa)
    if (null != endGrade) {
      if (endIsGaWhenFreeListening && grade.freeListening) {
        return addDelta(gaGrade, endGrade.score, state)
      }
      if (!hasDelta(gaGrade) && endGrade.score.isDefined &&
        java.lang.Float.compare(endGrade.score.get, minEndScore) < 0) {
        return addDelta(gaGrade, endGrade.score, state)
      }
    }
    var totalGa = 0f
    var totalPercent = 0f
    var scorePercent = 0f
    var hasEmptyEndGrade = false
    for (examGrade <- grade.examGrades if examGrade.gradeType != Delay) {
      if (examGrade.gradeType == End && null == examGrade.score) {
        hasEmptyEndGrade = true
      }
      getPercent(examGrade, grade, state) foreach { myPercent =>
        if (examGrade.score.isDefined ||
          (null != examGrade.examStatus && examGrade.examStatus.id != ExamStatus.Normal)) {
          totalPercent += myPercent
          examGrade.score foreach { score =>
            scorePercent += myPercent
            totalGa += (myPercent / 100.0f) * score
          }
        }
      }
    }
    if (totalPercent < 100) {
      if (totalPercent > 0) {
        null
      } else {
        gaGrade.score
      }
    } else {
      if (scorePercent <= 51 || hasEmptyEndGrade) {
        null
      } else {
        addDelta(gaGrade, Some(totalGa), state)
      }
    }
  }

  override def calcMakeupDelayGa(grade: CourseGrade, state: CourseGradeState): GaGrade = {
    val stdId = grade.std.id
    grade.std = entityDao.get(classOf[Student], stdId)
    val gatypes = List(DelayGa, MakeupGa)
    var makeupDelayGa: GaGrade = null
    var makeupDelayGrade:Option[ExamGrade]  = None
    for (gatype <- gatypes) {
      val gag = getGaGrade(grade, gatype)
      var gaScore: Option[Float] = None
      if (gatype == DelayGa){
        gaScore=calcDelayGaScore(grade, state)
        makeupDelayGrade = grade.getExamGrade(Delay)
      } else {
        gaScore=calcMakeupGaScore(grade, state)
        makeupDelayGrade = grade.getExamGrade(Makeup)
      }
      if (gaScore.isEmpty && (makeupDelayGrade.isEmpty || makeupDelayGrade.get.id == ExamStatus.Normal)) {
        grade.getGaGrade(gatype) foreach { gaGrade => grade.gaGrades -= gaGrade }
      } else {
        updateScore(gag, gaScore, null)
        if (!gag.published && null != state) gag.status = state.getStatus(gag.gradeType)
        makeupDelayGa = gag
      }
    }
    if (null != makeupDelayGa) {
      if (!grade.published) grade.status = guessFinalStatus(grade)
      if (makeupDelayGa.status == grade.status) {
        updateScore(grade, calcScore(grade, state), null)
      }
    }
    makeupDelayGa
  }

  private def getPercent(eg: ExamGrade, cg: CourseGrade, cgs: CourseGradeState): Option[Short] = {
    if (eg.scorePercent.isDefined) return eg.scorePercent
    if (eg.gradeType == Delay) {
      val end = cg.getExamGrade(End)
      if (end.isDefined && end.get.scorePercent.isDefined) {
        end.get.scorePercent
      } else {
        if (null == cgs) None else cgs.getPercent(End)
      }
    } else {
      if (null == cgs) None else cgs.getPercent(eg.gradeType)
    }
  }

  protected def calcDelayGaScore(grade: CourseGrade, state: CourseGradeState): Option[Float] = {
    var gascore: Option[Float] = None
    var gaGrade = grade.getGaGrade(DelayGa).orNull
    if (gaGrade != null) {
      gascore = gaGrade.score
      if (grade.getExamGrade(Delay).isEmpty) return gascore
    }
    val deGrade = grade.getExamGrade(Delay).orNull
    if (deGrade == null) return None
    if (null != deGrade.examStatus && deGrade.examStatus.cheating) return Some(0f)

    val setting = settings.getSetting(grade.project)
    if (setting.delayIsGa) return deGrade.score

    gaGrade = getGaGrade(grade, DelayGa)
    if (endIsGaWhenFreeListening && grade.freeListening) {
      return addDelta(gaGrade, deGrade.score, state)
    }
    if (!hasDelta(gaGrade) && deGrade.score.isDefined && java.lang.Float.compare(deGrade.score.get, minEndScore) < 0) {
      return addDelta(gaGrade, deGrade.score, state)
    }
    var ga = 0f
    var totalPercent = 0f
    var scorePercent = 0f
    for (examGrade <- grade.examGrades; if examGrade.gradeType != End) {
      getPercent(examGrade, grade, state) foreach { myPercent =>
        if (null != examGrade.score ||
          (null != examGrade.examStatus && examGrade.examStatus.id != ExamStatus.Normal)) {
          totalPercent += myPercent
          examGrade.score foreach { score =>
            scorePercent += myPercent
            ga += (myPercent / 100.0f) * score
          }
        }
      }
    }
    if (totalPercent < 100) {
      None
    } else {
      if (scorePercent < 51) null else addDelta(gaGrade, Some(ga), state)
    }
  }

  protected def calcMakeupGaScore(grade: CourseGrade, gradeState: CourseGradeState): Option[Float] = {
    var gascore: Option[Float] = None
    var gaGrade = grade.getGaGrade(MakeupGa).orNull
    if (gaGrade != null) {
      gascore = gaGrade.score
      if (grade.getExamGrade(Makeup).isEmpty) return gascore
    }
    val makeup = grade.getExamGrade(Makeup).orNull
    if (null == makeup || makeup.score.isEmpty) return None
    if (null != makeup.examStatus && makeup.examStatus.cheating) return Some(0f)
    gaGrade = getGaGrade(grade, MakeupGa)
    addDelta(gaGrade, makeup.score, gradeState)
    if (java.lang.Float.compare(gaGrade.score.get, 60) >= 0) Some(60f) else gaGrade.score
  }

  protected def calcScore(grade: CourseGrade, state: CourseGradeState): Option[Float] = {
    var best: Option[Float] = None
    for (gg <- grade.gaGrades if gg.score.isDefined) {
      var myScore: Option[Float] = None
      if (gg.gradeType != EndGa) {
        if (gg.published) myScore = gg.score
      } else {
        myScore = gg.score
      }
      myScore.foreach { ms =>
        best match {
          case None => best = myScore
          case Some(b) => if (ms.compareTo(b) > 0) best = Some(ms)
        }
      }
    }
    best
  }

  private def getGaGrade(grade: CourseGrade, gradeType: GradeType): GaGrade = {
    grade.getGaGrade(gradeType) match {
      case None =>
        val gaGrade = new GaGrade
        gaGrade.gradingMode = grade.gradingMode
        gaGrade.gradeType = gradeType
        gaGrade.createdAt = Instant.now
        gaGrade.updatedAt = Instant.now
        grade.addGaGrade(gaGrade)
        gaGrade
      case Some(gaGrade) => gaGrade
    }
  }

  private def guessFinalStatus(grade: CourseGrade): Int = {
    var status = Grade.Status.New
    grade.getGaGrade(EndGa) foreach { ga =>
      if (ga.status > status) status = ga.status
    }
    grade.getGaGrade(MakeupGa) foreach { ga =>
      if (ga.status > status) status = ga.status
    }
    grade.getGaGrade(DelayGa) foreach { ga =>
      if (ga.status > status) status = ga.status
    }
    status
  }

  def updateScore(grade: CourseGrade, score: Option[Float], newStyle: GradingMode): Unit = {
    var gradingMode = newStyle
    if (null == gradingMode) gradingMode = grade.gradingMode else grade.gradingMode = gradingMode
    val converter = gradeRateService.getConverter(grade.project, gradingMode)
    grade.score = score
    grade.scoreText = converter.convert(score)
    if (null != grade.courseTakeType &&
      grade.courseTakeType.id == CourseTakeType.Exemption) {
      grade.passed = true
    } else {
      grade.passed = converter.passed(grade.score)
    }
    grade.gp = converter.calcGp(grade.score)
    grade.operator = Some(Securities.user)
    grade.updatedAt = Instant.now
  }

  def updateScore(eg: ExamGrade, score: Option[Float], newStyle: GradingMode): Unit = {
    eg.score = score
    var gradingMode = newStyle
    if (null == gradingMode) gradingMode = eg.gradingMode else eg.gradingMode = gradingMode
    val converter = gradeRateService.getConverter(eg.courseGrade.project, eg.gradingMode)
    eg.scoreText = converter.convert(eg.score)
    eg.passed = converter.passed(eg.score)
    eg.updatedAt = Instant.now
    eg.operator = Some(Securities.user)
  }

  def updateScore(gag: GaGrade, score: Option[Float], newStyle: GradingMode): Unit = {
    gag.score = score
    var gradingMode = newStyle
    if (null == gradingMode) gradingMode = gag.gradingMode else gag.gradingMode = gradingMode
    val converter = gradeRateService.getConverter(gag.courseGrade.project, gradingMode)
    gag.scoreText = converter.convert(gag.score)
    gag.passed = converter.passed(gag.score)
    gag.updatedAt = Instant.now
    gag.operator = Some(Securities.user)
    gag.gp = converter.calcGp(gag.score)
  }

  protected def hasDelta(gaGrade: GaGrade): Boolean = {
    gaGrade.delta.isDefined
  }

  protected def getDelta(gaGrade: GaGrade, score: Option[Float], state: CourseGradeState): Float = {
    gaGrade.delta.getOrElse(0f)
  }

  private def addDelta(gaGrade: GaGrade, score: Option[Float], state: CourseGradeState): Option[Float] = {
    if (score.isEmpty) return None
    val delta = getDelta(gaGrade, score, state)
    val ga = reserve(delta + score.get, state)
    gaGrade.score = Some(ga)
    gaGrade.score
  }

  protected def reserve(score: Float, state: CourseGradeState): Float = {
    this.numRounder.round(score, 0) //state.precision)
  }

}
