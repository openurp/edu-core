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

package org.openurp.edu.grade.service.audit

import org.beangle.commons.collection.Collections
import org.beangle.commons.logging.Logging
import org.openurp.base.edu.model.Course
import org.openurp.edu.grade.model.{AuditCourseResult, AuditGroupResult, AuditPlanResult}

import java.time.Instant

/**
 * 计划审核保存
 *
 */
object AuditPlanResultMerger extends Logging {

  def merge(newResult: AuditPlanResult, existedResult: AuditPlanResult): AuditPlanResult = {
    if (existedResult.archived) {
      throw new RuntimeException(s"Cannot merge into a archived audit plan results ${existedResult.id}")
    }
    existedResult.remark = newResult.remark
    existedResult.updatedAt = Instant.now
    existedResult.requiredCredits = newResult.requiredCredits
    existedResult.owedCredits = newResult.owedCredits
    existedResult.owedCredits2 = newResult.owedCredits2
    existedResult.owedCredits3 = newResult.owedCredits3
    existedResult.passedCredits = newResult.passedCredits
    existedResult.passed = newResult.passed
    existedResult.predicted = newResult.predicted
    val updates = new StringBuilder()

    mergePlanResult(existedResult, newResult, updates)
    // delete last ';'
    if (updates.nonEmpty) updates.deleteCharAt(updates.length - 1)
    existedResult.updates = Some(updates.toString)
    existedResult
  }

  private def mergePlanResult(target: AuditPlanResult, source: AuditPlanResult, updates: StringBuilder): Unit = {
    // 收集课程组[groupName->groupResult]
    val tarGroupResults = Collections.newMap[String, AuditGroupResult]
    val sourceGroupResults = Collections.newMap[String, AuditGroupResult]
    val tarTops = target.topGroupResults
    val srcTops = source.topGroupResults
    for (result <- tarTops) tarGroupResults.put(result.name, result)
    //删除原有计划内相同名称重复组
    for (result <- tarTops) {
      if (!tarGroupResults.get(result.name).contains(result)) {
        result.parent foreach { p =>
          p.removeChild(result)
        }
        target.removeGroupResult(result)
      }
    }
    for (result <- srcTops) sourceGroupResults.put(result.name, result)

    // 删除没有的课程组
    val removed = Collections.subtract(tarGroupResults.keySet, sourceGroupResults.keySet)
    for (groupName <- removed) {
      tarGroupResults(groupName).detach()
    }
    // 添加课程组
    val added = Collections.subtract(sourceGroupResults.keySet, tarGroupResults.keySet)
    for (groupName <- added) {
      sourceGroupResults(groupName).attachTo(target)
    }
    // 合并课程组
    val common = Collections.intersection(sourceGroupResults.keySet, tarGroupResults.keySet)
    for (groupName <- common) {
      mergeGroupResult(target, tarGroupResults(groupName), sourceGroupResults(groupName), updates)
    }
  }

  private def mergeGroupResult(existedResult: AuditPlanResult, target: AuditGroupResult, source: AuditGroupResult, updates: StringBuilder): Unit = {
    // 统计完成学分的变化
    val delta = source.passedCredits - target.passedCredits
    if (java.lang.Float.compare(delta, 0) != 0) {
      updates.append(source.name)
      if (delta > 0) updates.append('+').append(delta) else updates.append(delta)
      updates.append(';')
    }
    target.indexno = source.indexno
    target.requiredCredits = source.requiredCredits
    target.subCount = source.subCount

    target.passed = source.passed
    target.convertedCredits = source.convertedCredits
    target.passedCredits = source.passedCredits
    target.owedCredits = source.owedCredits
    target.owedCredits2 = source.owedCredits2
    target.owedCredits3 = source.owedCredits3
    // 收集课程组[groupName->groupResult]
    val tarGroupResults = Collections.newMap[String, AuditGroupResult]
    val sourceGroupResults = Collections.newMap[String, AuditGroupResult]
    for (result <- target.children) tarGroupResults.put(result.name, result)
    for (result <- source.children) sourceGroupResults.put(result.name, result)

    // 收集课程结果[course->courseResult]
    val tarCourseResults = Collections.newMap[Course, AuditCourseResult]
    val sourceCourseResults = Collections.newMap[Course, AuditCourseResult]
    for (courseResult <- target.courseResults) tarCourseResults.put(courseResult.course, courseResult)
    for (courseResult <- source.courseResults) sourceCourseResults.put(courseResult.course, courseResult)

    // 删除没有的课程组
    val removed = Collections.subtract(tarGroupResults.keySet, sourceGroupResults.keySet)
    for (groupName <- removed) {
      val gg = tarGroupResults(groupName)
      gg.detach()
      target.removeChild(gg)
    }
    // 添加课程组
    val added = Collections.subtract(sourceGroupResults.keySet, tarGroupResults.keySet)
    for (groupName <- added) {
      val groupResult = sourceGroupResults(groupName)
      target.addChild(groupResult)
      groupResult.attachTo(existedResult)
    }
    // 合并课程组
    val common = Collections.intersection(sourceGroupResults.keySet, tarGroupResults.keySet)
    for (groupName <- common) {
      mergeGroupResult(existedResult, tarGroupResults(groupName), sourceGroupResults(groupName),
        updates)
    }
    // ------- 合并课程结果
    // 删除没有的课程
    val removedCourses = Collections.subtract(tarCourseResults.keySet, sourceCourseResults.keySet)
    for (course <- removedCourses) {
      val courseResult = tarCourseResults(course)
      target.courseResults -= courseResult
    }
    // 添加新的课程结果
    val addedCourses = Collections.subtract(sourceCourseResults.keySet, tarCourseResults.keySet)
    for (course <- addedCourses) {
      val courseResult = sourceCourseResults(course)
      courseResult.groupResult.courseResults -= courseResult
      courseResult.groupResult = target
      target.courseResults += courseResult
    }
    // 合并共同的课程
    val commonCourses = Collections.intersection(sourceCourseResults.keySet, tarCourseResults.keySet)
    for (course <- commonCourses) {
      val tar = tarCourseResults(course)
      val src = sourceCourseResults(course)
      tar.passed = src.passed
      tar.scores = src.scores
      tar.compulsory = src.compulsory
      tar.hasGrade = src.hasGrade
      tar.predicted = src.predicted
      tar.taking = src.taking
      tar.passedWay = src.passedWay
      tar.terms = src.terms
      tar.remark = src.remark
    }
  }
}
